package com.jk.amazon2.posting.controller;

import com.jk.amazon2.posting.dto.BatchStatusResponse;
import com.jk.amazon2.posting.dto.ErrorLogDto;
import com.jk.amazon2.posting.dto.StatisticsResponse;
import com.jk.amazon2.posting.entity.*;
import com.jk.amazon2.posting.repository.*;
import com.jk.amazon2.posting.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/postings")
@RequiredArgsConstructor
public class MonitoringController {

    private final BatchExecutionRepository batchExecutionRepository;
    private final PostingErrorRepository errorRepository;
    private final PostingDeadLetterRepository deadLetterRepository;
    private final StatisticsService statisticsService;

    @GetMapping("/batch/status")
    public ResponseEntity<BatchStatusResponse> getBatchStatus() {
        Optional<BatchExecution> lastExecution = batchExecutionRepository.findLatestExecution();

        BatchStatusResponse.LastExecution lastExec = lastExecution
            .map(be -> new BatchStatusResponse.LastExecution(
                be.getId(),
                be.getBatchType(),
                be.getStartDate(),
                be.getEndDate(),
                be.getStatus(),
                be.getTotalCount(),
                be.getSuccessCount(),
                be.getRetryCount(),
                be.getFailedCount(),
                be.getCompletedAt()
            ))
            .orElse(null);

        long totalDeadLetters = deadLetterRepository.count();
        long totalErrors = errorRepository.count();
        double successRate = lastExecution
            .map(be -> be.getTotalCount() > 0 ?
                (be.getSuccessCount() / (double) be.getTotalCount()) * 100 : 0.0)
            .orElse(0.0);

        BatchStatusResponse.CurrentStats stats = new BatchStatusResponse.CurrentStats(
            totalDeadLetters,
            totalErrors,
            successRate
        );

        return ResponseEntity.ok(new BatchStatusResponse(lastExec, stats));
    }

    @GetMapping("/errors")
    public ResponseEntity<Page<ErrorLogDto>> getErrors(Pageable pageable) {
        Page<PostingError> errors = errorRepository.findAllByOrderByCreatedAtDesc(pageable);

        Page<ErrorLogDto> dtos = errors.map(error -> new ErrorLogDto(
            error.getId(),
            error.getMemberId(),
            "",
            error.getTargetDate(),
            error.getDayOfWeek(),
            error.getErrorMessage(),
            error.getRetryCount(),
            error.getCreatedAt()
        ));

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/dead-letters")
    public ResponseEntity<Page<ErrorLogDto>> getDeadLetters(Pageable pageable) {
        Page<PostingDeadLetter> deadLetters = deadLetterRepository.findAllByOrderByCreatedAtDesc(pageable);

        Page<ErrorLogDto> dtos = deadLetters.map(dl -> new ErrorLogDto(
            dl.getId(),
            dl.getMemberId(),
            "",
            dl.getTargetDate(),
            dl.getDayOfWeek(),
            dl.getErrorMessage(),
            3,
            dl.getCreatedAt()
        ));

        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/errors/{errorId}/retry")
    public ResponseEntity<Map<String, String>> retryError(@PathVariable Long errorId) {
        Optional<PostingError> error = errorRepository.findById(errorId);

        if (error.isPresent()) {
            PostingError err = error.get();
            err.incrementRetryCount();
            errorRepository.save(err);
        }

        return ResponseEntity.ok(Map.of("status", "QUEUED", "message", "재시도 요청이 큐에 추가됨"));
    }

    @PostMapping("/dead-letters/{deadLetterId}/retry")
    public ResponseEntity<Map<String, String>> retryDeadLetter(@PathVariable Long deadLetterId) {
        Optional<PostingDeadLetter> deadLetter = deadLetterRepository.findById(deadLetterId);

        if (deadLetter.isPresent()) {
            deadLetterRepository.delete(deadLetter.get());
        }

        return ResponseEntity.ok(Map.of("status", "QUEUED", "message", "재시도 요청이 큐에 추가됨"));
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate
    ) {
        StatisticsResponse stats = statisticsService.getStatistics(startDate, endDate);
        return ResponseEntity.ok(stats);
    }
}
