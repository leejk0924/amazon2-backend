package com.jk.amazon2.posting.integration;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.entity.MonthlyPostingSummary;
import com.jk.amazon2.posting.repository.MonthlyPostingSummaryRepository;
import com.jk.amazon2.posting.service.PostingService;
import com.jk.amazon2.testsupport.TestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestContainerConfig.class)
@DisplayName("[통합] 월별 포스팅 집계 통계 갱신 통합 테스트")
class MonthlyPostingSummaryIntegrationTest {

    @Autowired
    private PostingService postingService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MonthlyPostingSummaryRepository summaryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        jdbcTemplate.execute("TRUNCATE TABLE monthly_posting_summary");
        jdbcTemplate.execute("TRUNCATE TABLE posting");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    @Test
    @DisplayName("[통합] 포스팅 저장 시 해당 월 요약이 생성된다 [success]")
    void savePosting_시_월별_요약_생성() {
        // Given
        Member member = memberRepository.save(Member.of("user-a", "멤버A", "TECH"));
        LocalDate weekStart = LocalDate.of(2026, 6, 1);

        // When
        postingService.savePosting(member.getId(), weekStart, 1, 2, 3, 4, 5, 6, 7, "admin");

        // Then
        Optional<MonthlyPostingSummary> summary = summaryRepository.findAll().stream()
            .filter(s -> s.getMemberId().equals(member.getId()))
            .findFirst();
        assertThat(summary).isPresent();
        assertThat(summary.get().getSummaryMonth()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(summary.get().getTotalCount()).isEqualTo(28);
    }

    @Test
    @DisplayName("[통합] 같은 달의 다른 주 포스팅도 합산된다 [success]")
    void savePosting_같은_달_여러_주_합산() {
        // Given
        Member member = memberRepository.save(Member.of("user-b", "멤버B", "TECH"));

        // When
        postingService.savePosting(member.getId(), LocalDate.of(2026, 6, 1), 1, 1, 1, 1, 1, 1, 1, "admin"); // 7
        postingService.savePosting(member.getId(), LocalDate.of(2026, 6, 8), 2, 2, 2, 2, 2, 2, 2, "admin"); // 14

        // Then
        MonthlyPostingSummary summary = summaryRepository.findAll().stream()
            .filter(s -> s.getMemberId().equals(member.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(summary.getTotalCount()).isEqualTo(21);
    }

    @Test
    @DisplayName("[통합] 기존 주차 카운트를 정정하면 요약도 함께 재계산된다 [success]")
    void savePosting_정정_시_요약_재계산() {
        // Given
        Member member = memberRepository.save(Member.of("user-c", "멤버C", "TECH"));
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        postingService.savePosting(member.getId(), weekStart, 3, 5, 0, 0, 0, 0, 0, "admin"); // 8

        // When
        postingService.savePosting(member.getId(), weekStart, 3, 4, 0, 0, 0, 0, 0, "admin"); // 7 (tue 정정)

        // Then
        MonthlyPostingSummary summary = summaryRepository.findAll().stream()
            .filter(s -> s.getMemberId().equals(member.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(summary.getTotalCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("[통합] 같은 회원+월 조합은 중복 행 없이 한 행만 유지된다 [success]")
    void savePosting_유니크_제약_한_행만_유지() {
        // Given
        Member member = memberRepository.save(Member.of("user-d", "멤버D", "TECH"));

        // When
        postingService.savePosting(member.getId(), LocalDate.of(2026, 6, 1), 1, 0, 0, 0, 0, 0, 0, "admin");
        postingService.savePosting(member.getId(), LocalDate.of(2026, 6, 8), 1, 0, 0, 0, 0, 0, 0, "admin");

        // Then
        List<MonthlyPostingSummary> rows = summaryRepository.findAll().stream()
            .filter(s -> s.getMemberId().equals(member.getId()))
            .toList();
        assertThat(rows).hasSize(1);
    }
}
