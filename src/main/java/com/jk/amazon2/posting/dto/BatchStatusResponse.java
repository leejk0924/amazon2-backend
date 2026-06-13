package com.jk.amazon2.posting.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BatchStatusResponse(
    LastExecution lastExecution,
    CurrentStats currentStats
) {
    public record LastExecution(
        Long batchExecutionId,
        String batchType,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer totalCount,
        Integer successCount,
        Integer retryCount,
        Integer failedCount,
        LocalDateTime completedAt
    ) {}

    public record CurrentStats(
        Long totalDeadLetters,
        Long totalErrors,
        Double successRate
    ) {}
}
