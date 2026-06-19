package com.jk.amazon2.posting.service;

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
        try {
            Integer count = scraper.scrapePostingCount(task.memberId().toString(), task.targetDate());
            LocalDate weekStart = getWeekStartDate(task.targetDate());
            updatePostingForDay(task.memberId(), weekStart, task.dayOfWeek(), count);

            execution.incrementSuccessCount();
            batchExecutionRepository.save(execution);

            log.info("[BATCH] Saved posting - member={}, week={}, day={}, count={}",
                    task.memberId(), weekStart, task.dayOfWeek(), count);

        } catch (ParsingException | ScrapingException e) {
            handleTaskError(task, queue, execution, e);
        }
    }

    private void handleTaskError(BatchService.PostingTask task, BlockingQueue<BatchService.PostingTask> queue,
                                 BatchExecution execution, Exception e) {
        errorHandler.handleError(task.memberId(), task.targetDate(), task.dayOfWeek(), e);

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
        log.warn("[BATCH] Failed member={}, date={}, error={}", task.memberId(), task.targetDate(), e.getMessage());
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