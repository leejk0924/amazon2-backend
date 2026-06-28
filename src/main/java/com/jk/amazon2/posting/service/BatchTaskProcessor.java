package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import com.jk.amazon2.posting.repository.PostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTaskProcessor {

    private final BatchExecutionRepository batchExecutionRepository;
    private final PostingService postingService;
    private final PostingErrorRepository postingErrorRepository;
    private final ErrorHandler errorHandler;
    private final NaverBlogScraper scraper;
    private final PostingRepository postingRepository;

    @Transactional
    public void processTask(BatchService.PostingTask task, BlockingQueue<BatchService.PostingTask> queue, BatchExecution execution) {
        ScrapingResult<Integer> result = scraper.scrapePostingCount(task.memberNickname(), task.targetDate());

        switch (result) {
            case ScrapingResult.Success<Integer> success -> {
                LocalDate weekStart = getWeekStartDate(task.targetDate());
                updatePostingForDay(task.memberId(), weekStart, task.dayOfWeek(), success.value());
                execution.incrementSuccessCount();
                batchExecutionRepository.save(execution);
                log.debug("[BATCH] Saved posting - member={}, week={}, day={}, count={}",
                        task.memberId(), weekStart, task.dayOfWeek(), success.value());
            }
            case ScrapingResult.Failure<Integer> failure -> {
                switch (failure.type()) {
                    case PARSING_ERROR -> {
                        // 구조적 문제 → 재시도 없이 즉시 실패 처리
                        execution.incrementFailedCount();
                        batchExecutionRepository.save(execution);
                        log.error("[BATCH] 파싱 오류 (재시도 불가) member={}, date={}, error={}",
                                task.memberId(), task.targetDate(), failure.message());
                    }
                    case NETWORK_ERROR, HTTP_ERROR -> handleRetryableError(task, queue, execution, failure);
                    default -> {
                        // 분류되지 않은 새 FailureType 추가 시 유실 방지
                        execution.incrementFailedCount();
                        batchExecutionRepository.save(execution);
                        log.error("[BATCH] 미분류 실패 타입 (처리 누락 주의) type={}, member={}, date={}, error={}",
                                failure.type(), task.memberId(), task.targetDate(), failure.message());
                    }
                }
            }
        }
    }

    private void handleRetryableError(BatchService.PostingTask task, BlockingQueue<BatchService.PostingTask> queue,
                                      BatchExecution execution, ScrapingResult.Failure<Integer> failure) {
        RuntimeException cause = failure.cause() != null
                ? new RuntimeException(failure.message(), failure.cause())
                : new RuntimeException(failure.message());
        errorHandler.handleError(task.memberId(), task.targetDate(), task.dayOfWeek(), cause);

        PostingError error = postingErrorRepository
                .findByMemberAndDate(task.memberId(), task.targetDate())
                .stream()
                .filter(err -> err.getDayOfWeek().equals(task.dayOfWeek()))
                .findFirst()
                .orElse(null);

        if (error != null && error.getRetryCount() < 3) {
            execution.incrementRetryCount();
            try {
                queue.put(task);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } else if (error != null) {
            execution.incrementFailedCount();
            errorHandler.handleRetry(error);
        }

        batchExecutionRepository.save(execution);
        log.warn("[BATCH] Failed member={}, date={}, type={}, error={}",
                task.memberId(), task.targetDate(), failure.type(), failure.message());
    }

    private void updatePostingForDay(Long memberId, LocalDate weekStart, String dayOfWeek, Integer count) {
        Posting existing = postingRepository.findByMemberIdAndWeekStartDateWithLock(memberId, weekStart)
                .orElseGet(() -> createNewPosting(memberId, weekStart));

        switch (dayOfWeek) {
            case "mon" -> existing.setMon(count);
            case "tue" -> existing.setTue(count);
            case "wed" -> existing.setWed(count);
            case "thu" -> existing.setThu(count);
            case "fri" -> existing.setFri(count);
            case "sat" -> existing.setSat(count);
            case "sun" -> existing.setSun(count);
        }

        postingService.savePosting(memberId, weekStart,
                existing.getMon(), existing.getTue(), existing.getWed(),
                existing.getThu(), existing.getFri(), existing.getSat(),
                existing.getSun(), "batch");
    }

    private Posting createNewPosting(Long memberId, LocalDate weekStart) {
        return new Posting(memberId, weekStart, 0, 0, 0, 0, 0, 0, 0, "batch");
    }

    private LocalDate getWeekStartDate(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.minusDays(1);
        }
        return date;
    }
}
