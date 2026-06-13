package com.jk.amazon2.posting.service;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.exception.ParsingException;
import com.jk.amazon2.posting.exception.ScrapingException;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import com.jk.amazon2.posting.repository.PostingRepository;
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
    private final PostingService postingService;
    private final PostingErrorRepository postingErrorRepository;
    private final ErrorHandler errorHandler;
    private final NaverBlogScraper scraper;
    private final RateLimiter rateLimiter;
    private final PostingRepository postingRepository;

    @Transactional
    public Long executeBatch(LocalDate startDate, LocalDate endDate, String batchType) {
        BatchExecution execution = new BatchExecution(batchType, startDate, endDate);
        batchExecutionRepository.save(execution);

        log.info("[BATCH] Posting batch started - type={}, period={}~{}",
            batchType, startDate, endDate);

        BlockingQueue<PostingTask> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            List<Member> members = memberRepository.findAll();
            prepareQueue(queue, members, startDate, endDate);

            executor.submit(() -> processQueue(queue, execution));

            try {
                waitForQueueEmpty(queue);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[BATCH] Queue wait interrupted", e);
            }

            execution.complete();
            batchExecutionRepository.save(execution);

            log.info("[BATCH] Posting batch completed - total={}, success={}, retry={}, failed={}",
                execution.getTotalCount(), execution.getSuccessCount(),
                execution.getRetryCount(), execution.getFailedCount());

        } catch (Exception e) {
            execution.fail();
            batchExecutionRepository.save(execution);
            log.error("[BATCH] Batch failed", e);
            throw e;
        } finally {
            executor.shutdown();
        }

        return execution.getId();
    }

    private void prepareQueue(BlockingQueue<PostingTask> queue, List<Member> members,
                             LocalDate startDate, LocalDate endDate) {
        for (Member member : members) {
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                String dayOfWeek = getDayOfWeekKr(current);
                PostingTask task = new PostingTask(
                    member.getId(),
                    member.getNickname(),
                    current,
                    dayOfWeek
                );

                try {
                    queue.put(task);
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
            processTask(task, queue, execution);
        }
    }

    private void processTask(PostingTask task, BlockingQueue<PostingTask> queue,
                            BatchExecution execution) {
        try {
            Integer count = scraper.scrapePostingCount(task.memberId.toString(), task.targetDate);

            LocalDate weekStart = getWeekStartDate(task.targetDate);
            updatePostingForDay(task.memberId, weekStart, task.dayOfWeek, count);

            execution.incrementSuccessCount();
            batchExecutionRepository.save(execution);

            log.info("[BATCH] Saved posting - member={}, week={}, day={}, count={}",
                task.memberId, weekStart, task.dayOfWeek, count);

        } catch (ParsingException | ScrapingException e) {
            handleTaskError(task, queue, execution, e);
        }
    }

    private void handleTaskError(PostingTask task, BlockingQueue<PostingTask> queue,
                                BatchExecution execution, Exception e) {
        errorHandler.handleError(task.memberId, task.targetDate, task.dayOfWeek, e);

        PostingError error = postingErrorRepository
            .findByMemberAndDate(task.memberId, task.targetDate)
            .stream()
            .filter(err -> err.getDayOfWeek().equals(task.dayOfWeek))
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
        log.warn("[BATCH] Failed member={}, date={}, error={}",
            task.memberId, task.targetDate, e.getMessage());
    }

    private void updatePostingForDay(Long memberId, LocalDate weekStart,
                                    String dayOfWeek, Integer count) {
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

    private void waitForQueueEmpty(BlockingQueue<PostingTask> queue) throws InterruptedException {
        while (!queue.isEmpty()) {
            Thread.sleep(100);
        }
    }

    record PostingTask(Long memberId, String memberNickname, LocalDate targetDate, String dayOfWeek) {}
}
