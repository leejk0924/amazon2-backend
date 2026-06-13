package com.jk.amazon2.posting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_execution",
       indexes = {
           @Index(name = "idx_started_at", columnList = "started_at"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_batch_type", columnList = "batch_type")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String batchType; // SCHEDULED, MANUAL

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Integer totalCount;

    @Column(nullable = false)
    private Integer successCount;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false)
    private Integer failedCount;

    @Column(nullable = false, length = 20)
    private String status; // IN_PROGRESS, COMPLETED, FAILED

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(nullable = true)
    private LocalDateTime completedAt;

    public BatchExecution(String batchType, LocalDate startDate, LocalDate endDate) {
        this.batchType = batchType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalCount = 0;
        this.successCount = 0;
        this.retryCount = 0;
        this.failedCount = 0;
        this.status = "IN_PROGRESS";
        this.startedAt = LocalDateTime.now();
    }

    public void incrementSuccessCount() {
        this.successCount++;
        this.totalCount++;
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.totalCount++;
    }

    public void incrementFailedCount() {
        this.failedCount++;
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = "FAILED";
        this.completedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
