package com.jk.amazon2.posting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "posting_dead_letter",
       indexes = {
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_member_date", columnList = "member_id,target_date")
       })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostingDeadLetter {

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

    @Column(nullable = true)
    private LocalDateTime lastRetryAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PostingDeadLetter(Long memberId, LocalDate targetDate, String dayOfWeek,
                            String errorMessage) {
        this.memberId = memberId;
        this.targetDate = targetDate;
        this.dayOfWeek = dayOfWeek;
        this.errorMessage = errorMessage;
        this.createdAt = LocalDateTime.now();
        this.lastRetryAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
