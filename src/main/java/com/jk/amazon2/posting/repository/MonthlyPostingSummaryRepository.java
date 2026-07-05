package com.jk.amazon2.posting.repository;

import com.jk.amazon2.posting.entity.MonthlyPostingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MonthlyPostingSummaryRepository extends JpaRepository<MonthlyPostingSummary, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO monthly_posting_summary (member_id, summary_month, total_count)
            VALUES (:memberId, :yearMonth, :total)
            ON DUPLICATE KEY UPDATE total_count = :total
            """, nativeQuery = true)
    void upsertTotal(@Param("memberId") Long memberId, @Param("yearMonth") LocalDate yearMonth, @Param("total") int total);

    @Query(value = """
            SELECT s.member_id AS memberId, m.nickname AS nickname, s.total_count AS totalCount
            FROM monthly_posting_summary s
            JOIN member m ON m.id = s.member_id
            WHERE s.summary_month = :yearMonth AND m.deleted = false
            ORDER BY s.total_count DESC, s.member_id ASC
            LIMIT 10
            """, nativeQuery = true)
    List<MonthlyRankingRow> findTop10ByYearMonth(@Param("yearMonth") LocalDate yearMonth);

    interface MonthlyRankingRow {
        Long getMemberId();
        String getNickname();
        Integer getTotalCount();
    }
}
