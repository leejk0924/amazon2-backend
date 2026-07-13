package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import com.jk.amazon2.posting.repository.PostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BatchTaskProcessor의 재시도 가능 실패(NETWORK_ERROR/HTTP_ERROR) 처리 로직 테스트.
 * #77: 재시도마다 posting_error에 중복 row가 쌓이고 retryCount가 증가하지 않던 버그의 회귀 테스트 포함.
 */
@ExtendWith(MockitoExtension.class)
class BatchTaskProcessorTest {

    @Mock
    private BatchExecutionRepository batchExecutionRepository;

    @Mock
    private PostingService postingService;

    @Mock
    private PostingErrorRepository postingErrorRepository;

    @Mock
    private ErrorHandler errorHandler;

    @Mock
    private NaverBlogScraper scraper;

    @Mock
    private PostingRepository postingRepository;

    private BatchTaskProcessor batchTaskProcessor;

    private static final Long MEMBER_ID = 93L;
    private static final LocalDate TARGET_DATE = LocalDate.of(2026, 7, 6);
    private static final String DAY_OF_WEEK = "mon";

    @BeforeEach
    void setUp() {
        batchTaskProcessor = new BatchTaskProcessor(
                batchExecutionRepository, postingService, postingErrorRepository,
                errorHandler, scraper, postingRepository
        );
    }

    private BatchService.PostingTask networkErrorTask() {
        return new BatchService.PostingTask(MEMBER_ID, "nickname", TARGET_DATE, DAY_OF_WEEK);
    }

    @Test
    @DisplayName("기존 에러 기록이 없을 때 재시도 가능한 실패가 발생하면 새 에러를 생성하고 재큐잉한다")
    void processTask_기존에러없음_새에러생성후_재큐잉() {
        // given
        BatchService.PostingTask task = networkErrorTask();
        BatchExecution execution = new BatchExecution("MANUAL", TARGET_DATE, TARGET_DATE);
        BlockingQueue<BatchService.PostingTask> queue = new LinkedBlockingQueue<>();

        when(scraper.scrapePostingCount(task.memberNickname(), task.targetDate()))
                .thenReturn(new ScrapingResult.Failure<>(ScrapingResult.FailureType.NETWORK_ERROR, "connection reset", null));
        when(postingErrorRepository.findByMemberAndDateAndDayOfWeek(MEMBER_ID, TARGET_DATE, DAY_OF_WEEK))
                .thenReturn(List.of());

        // when
        batchTaskProcessor.processTask(task, queue, execution);

        // then
        verify(errorHandler, times(1)).handleError(eq(MEMBER_ID), eq(TARGET_DATE), eq(DAY_OF_WEEK), any());
        verify(errorHandler, never()).handleRetry(any());
        assertThat(queue).containsExactly(task);
        assertThat(execution.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("기존 에러 기록이 있을 때는 새로 생성하지 않고 기존 기록의 재시도 횟수만 증가시킨다")
    void processTask_기존에러존재_중복생성없이_재시도횟수만증가() {
        // given
        BatchService.PostingTask task = networkErrorTask();
        BatchExecution execution = new BatchExecution("MANUAL", TARGET_DATE, TARGET_DATE);
        BlockingQueue<BatchService.PostingTask> queue = new LinkedBlockingQueue<>();
        PostingError existingError = new PostingError(MEMBER_ID, TARGET_DATE, DAY_OF_WEEK, "이전 에러", 1);

        when(scraper.scrapePostingCount(task.memberNickname(), task.targetDate()))
                .thenReturn(new ScrapingResult.Failure<>(ScrapingResult.FailureType.HTTP_ERROR, "500", null));
        when(postingErrorRepository.findByMemberAndDateAndDayOfWeek(MEMBER_ID, TARGET_DATE, DAY_OF_WEEK))
                .thenReturn(List.of(existingError));
        when(errorHandler.handleRetry(existingError)).thenReturn(ErrorHandler.RetryOutcome.RETRYABLE);

        // when
        batchTaskProcessor.processTask(task, queue, execution);

        // then
        verify(errorHandler, never()).handleError(any(), any(), any(), any());
        verify(errorHandler, times(1)).handleRetry(existingError);
        assertThat(queue).containsExactly(task);
        assertThat(execution.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("기존 에러가 Dead Letter로 이동하면 재큐잉하지 않고 실패로 집계한다")
    void processTask_deadLetter이동시_재큐잉하지않음() {
        // given
        BatchService.PostingTask task = networkErrorTask();
        BatchExecution execution = new BatchExecution("MANUAL", TARGET_DATE, TARGET_DATE);
        BlockingQueue<BatchService.PostingTask> queue = new LinkedBlockingQueue<>();
        PostingError existingError = new PostingError(MEMBER_ID, TARGET_DATE, DAY_OF_WEEK, "이전 에러", 2);

        when(scraper.scrapePostingCount(task.memberNickname(), task.targetDate()))
                .thenReturn(new ScrapingResult.Failure<>(ScrapingResult.FailureType.NETWORK_ERROR, "timeout", null));
        when(postingErrorRepository.findByMemberAndDateAndDayOfWeek(MEMBER_ID, TARGET_DATE, DAY_OF_WEEK))
                .thenReturn(List.of(existingError));
        when(errorHandler.handleRetry(existingError)).thenReturn(ErrorHandler.RetryOutcome.DEAD_LETTERED);

        // when
        batchTaskProcessor.processTask(task, queue, execution);

        // then
        assertThat(queue).isEmpty();
        assertThat(execution.getFailedCount()).isEqualTo(1);
        verify(errorHandler, never()).handleError(any(), any(), any(), any());
    }

    @Test
    @DisplayName("#77 회귀 테스트: 동일 작업이 반복 실패해도 posting_error 기록은 1건만 생성된다")
    void processTask_반복실패해도_중복에러row가_생성되지않는다() throws InterruptedException {
        // given: postingErrorRepository를 인메모리 리스트로 대체해 실제 조회/생성 흐름을 시뮬레이션
        BatchExecution execution = new BatchExecution("MANUAL", TARGET_DATE, TARGET_DATE);
        BlockingQueue<BatchService.PostingTask> queue = new LinkedBlockingQueue<>();
        List<PostingError> savedErrors = new ArrayList<>();
        BatchService.PostingTask task = networkErrorTask();

        when(scraper.scrapePostingCount(anyString(), eq(TARGET_DATE)))
                .thenReturn(new ScrapingResult.Failure<>(ScrapingResult.FailureType.NETWORK_ERROR, "connection reset", null));
        when(postingErrorRepository.findByMemberAndDateAndDayOfWeek(eq(MEMBER_ID), eq(TARGET_DATE), eq(DAY_OF_WEEK)))
                .thenAnswer(invocation -> new ArrayList<>(savedErrors));
        doAnswer(invocation -> {
            savedErrors.add(new PostingError(MEMBER_ID, TARGET_DATE, DAY_OF_WEEK, "err", 1));
            return null;
        }).when(errorHandler).handleError(eq(MEMBER_ID), eq(TARGET_DATE), eq(DAY_OF_WEEK), any());
        when(errorHandler.handleRetry(any(PostingError.class))).thenAnswer(invocation -> {
            PostingError error = invocation.getArgument(0);
            error.incrementRetryCount();
            if (error.getRetryCount() >= 3) {
                savedErrors.remove(error);
                return ErrorHandler.RetryOutcome.DEAD_LETTERED;
            }
            return ErrorHandler.RetryOutcome.RETRYABLE;
        });

        // when: 실제 배치 큐 소비 루프처럼 재큐잉된 작업을 계속 꺼내 처리 (최대 3회 실패 후 Dead Letter)
        queue.put(task);
        int iterations = 0;
        BatchService.PostingTask polled;
        while ((polled = queue.poll()) != null && iterations < 5) {
            batchTaskProcessor.processTask(polled, queue, execution);
            iterations++;
        }

        // then: 버그 이전에는 매 재시도마다 새 row가 insert되어 savedErrors가 계속 늘어났음
        assertThat(savedErrors).isEmpty();
        verify(errorHandler, times(1)).handleError(any(), any(), any(), any());
        verify(errorHandler, times(2)).handleRetry(any(PostingError.class));
        assertThat(queue).isEmpty();
        assertThat(execution.getRetryCount()).isEqualTo(2);
        assertThat(execution.getFailedCount()).isEqualTo(1);
    }
}
