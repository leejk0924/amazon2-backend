package com.jk.amazon2.posting.service;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private static final int QUEUE_CAPACITY = 1000;

    private final BatchExecutionRepository batchExecutionRepository;
    private final MemberRepository memberRepository;
    private final NaverBlogScraper scraper;
    private final RateLimiter rateLimiter;
    private final BatchTaskProcessor batchTaskProcessor;

    // @Transactional 제거 - async 스레드와 트랜잭션 경계를 분리
    public Long executeBatch(LocalDate startDate, LocalDate endDate, String batchType) {
        BatchExecution execution = createExecution(batchType, startDate, endDate);

        log.info("[BATCH] Posting batch started - type={}, period={}~{}",
                batchType, startDate, endDate);

        BlockingQueue<PostingTask> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            List<Member> members = memberRepository.findAll();
            prepareQueue(queue, members, startDate, endDate);

            Future<?> future = executor.submit(() -> processQueue(queue, execution));
            future.get(); // 스레드 완전 종료 보장

            completeExecution(execution);

            log.info("[BATCH] Posting batch completed - total={}, success={}, retry={}, failed={}",
                    execution.getTotalCount(), execution.getSuccessCount(),
                    execution.getRetryCount(), execution.getFailedCount());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failExecution(execution);
            log.error("[BATCH] Batch interrupted", e);
        } catch (ExecutionException e) {
            failExecution(execution);
            log.error("[BATCH] Batch failed", e.getCause());
        } finally {
            executor.shutdown();
        }

        return execution.getId();
    }

    @Transactional
    public BatchExecution createExecution(String batchType, LocalDate startDate, LocalDate endDate) {
        BatchExecution execution = new BatchExecution(batchType, startDate, endDate);
        return batchExecutionRepository.save(execution);
    }

    @Transactional
    public void completeExecution(BatchExecution execution) {
        execution.complete();
        batchExecutionRepository.save(execution);
    }

    @Transactional
    public void failExecution(BatchExecution execution) {
        execution.fail();
        batchExecutionRepository.save(execution);
    }

    private void prepareQueue(BlockingQueue<PostingTask> queue, List<Member> members,
                              LocalDate startDate, LocalDate endDate) {
        for (Member member : members) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                try {
                    queue.put(new PostingTask(member.getId(), member.getNickname(), current, getDayOfWeekKr(current)));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Queue interrupted while preparing", e);
                }
                current = current.plusDays(1);
            }
        }
    }

    private void processQueue(BlockingQueue<PostingTask> queue, BatchExecution execution) {
        PostingTask task;
        while ((task = queue.poll()) != null || !queue.isEmpty()) {
            if (task == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }
            rateLimiter.acquire();
            batchTaskProcessor.processTask(task, queue, execution);
        }
    }

    private String getDayOfWeekKr(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "mon";
            case TUESDAY -> "tue";
            case WEDNESDAY -> "wed";
            case THURSDAY -> "thu";
            case FRIDAY -> "fri";
            case SATURDAY -> "sat";
            case SUNDAY -> "sun";
        };
    }

    public record PostingTask(Long memberId, String memberNickname, LocalDate targetDate, String dayOfWeek) {}
}