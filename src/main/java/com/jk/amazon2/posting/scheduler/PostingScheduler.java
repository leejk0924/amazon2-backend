package com.jk.amazon2.posting.scheduler;

import com.jk.amazon2.posting.service.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.DayOfWeek;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostingScheduler {

    private final BatchService batchService;

    /**
     * 매주 월요일 자정(00:00)에 지난주 데이터 수집
     */
    @Scheduled(cron = "0 0 0 ? * MON")
    public void scheduleWeeklyBatch() {
        log.info("[SCHEDULER] Weekly batch execution started");

        LocalDate today = LocalDate.now();
        LocalDate weekStartDate = getLastMonday(today);
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        try {
            batchService.executeBatch(weekStartDate, weekEndDate, "SCHEDULED");
            log.info("[SCHEDULER] Weekly batch completed successfully");
        } catch (Exception e) {
            log.error("[SCHEDULER] Weekly batch failed", e);
        }
    }

    private LocalDate getLastMonday(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.minusDays(1);
        }
        return date.minusDays(7); // 지난주 월요일
    }
}
