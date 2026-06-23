package com.jk.amazon2.posting.controller;

import com.jk.amazon2.posting.dto.BatchStatusResponse;
import com.jk.amazon2.posting.dto.ErrorLogDto;
import com.jk.amazon2.posting.dto.StatisticsResponse;
import com.jk.amazon2.posting.dto.WeeklyStatisticsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.jk.amazon2.posting.service.MonitoringService;
import com.jk.amazon2.posting.service.StatisticsService;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/postings")
@RequiredArgsConstructor
public class MonitoringController implements MonitoringApiSpec {

    private final MonitoringService monitoringService;
    private final StatisticsService statisticsService;

    @GetMapping("/batch/status")
    public ResponseEntity<BatchStatusResponse> getBatchStatus() {
        BatchStatusResponse response = monitoringService.getBatchStatus();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/errors")
    public ResponseEntity<Page<ErrorLogDto>> getErrors(Pageable pageable) {
        Page<ErrorLogDto> errors = monitoringService.getErrors(pageable);
        return ResponseEntity.ok(errors);
    }

    @GetMapping("/dead-letters")
    public ResponseEntity<Page<ErrorLogDto>> getDeadLetters(Pageable pageable) {
        Page<ErrorLogDto> deadLetters = monitoringService.getDeadLetters(pageable);
        return ResponseEntity.ok(deadLetters);
    }

    @PostMapping("/errors/{errorId}/retry")
    public ResponseEntity<Map<String, String>> retryError(@PathVariable Long errorId) {
        Map<String, String> response = monitoringService.retryError(errorId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/dead-letters/{deadLetterId}/retry")
    public ResponseEntity<Map<String, String>> retryDeadLetter(@PathVariable Long deadLetterId) {
        Map<String, String> response = monitoringService.retryDeadLetter(deadLetterId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate
    ) {
        StatisticsResponse stats = statisticsService.getStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/weekly-statistics")
    public ResponseEntity<WeeklyStatisticsResponse> getWeeklyStatistics(
        @RequestParam LocalDate weekStartDate
    ) {
        WeeklyStatisticsResponse stats = statisticsService.getWeeklyStatistics(weekStartDate);
        return ResponseEntity.ok(stats);
    }
}
