package com.jk.amazon2.posting.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "monthly_posting_summary",
       uniqueConstraints = @UniqueConstraint(name = "uk_member_month", columnNames = {"member_id", "summary_month"}))
public class MonthlyPostingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 해당 월의 1일로 정규화된 값 (예: 2026-06-01)
    @Column(name = "summary_month", nullable = false)
    private LocalDate summaryMonth;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;
}
