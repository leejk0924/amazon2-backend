package com.jk.amazon2.posting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "posting_error",
       indexes = {
           @Index(name = "idx_retry_count", columnList = "retry_count"),
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_member_date", columnList = "member_id,target_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDate targetDate;

    @Column(nullable = false, length = 10)
    private String dayOfWeek;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PostingError(Long memberId, LocalDate targetDate, String dayOfWeek,
                       String errorMessage, Integer retryCount) {
        this.memberId = memberId;
        this.targetDate = targetDate;
        this.dayOfWeek = dayOfWeek;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.createdAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
