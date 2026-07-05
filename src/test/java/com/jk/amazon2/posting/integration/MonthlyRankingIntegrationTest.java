package com.jk.amazon2.posting.integration;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.dto.MonthlyRankingResponse;
import com.jk.amazon2.posting.repository.MonthlyPostingSummaryRepository;
import com.jk.amazon2.posting.service.StatisticsService;
import com.jk.amazon2.testsupport.TestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestContainerConfig.class)
@Transactional
@DisplayName("[통합] 월별 포스팅 랭킹 조회 통합 테스트")
class MonthlyRankingIntegrationTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MonthlyPostingSummaryRepository summaryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final LocalDate YEAR_MONTH = LocalDate.of(2026, 6, 1);

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        jdbcTemplate.execute("TRUNCATE TABLE monthly_posting_summary");
        jdbcTemplate.execute("TRUNCATE TABLE posting");
        jdbcTemplate.execute("TRUNCATE TABLE member");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    @Test
    @DisplayName("[통합] 탈퇴한 회원은 랭킹에서 제외된다 [success]")
    void getMonthlyRanking_탈퇴회원_제외() {
        // Given
        Member active = memberRepository.save(Member.of("active-user", "활성", "TECH"));
        Member deleted = memberRepository.save(Member.of("deleted-user", "탈퇴", "TECH"));
        deleted.softDelete();
        memberRepository.save(deleted);

        summaryRepository.upsertTotal(active.getId(), YEAR_MONTH, 10);
        summaryRepository.upsertTotal(deleted.getId(), YEAR_MONTH, 100);

        // When
        MonthlyRankingResponse result = statisticsService.getMonthlyRanking(YEAR_MONTH);

        // Then
        assertThat(result.rankings()).hasSize(1);
        assertThat(result.rankings().get(0).nickname()).isEqualTo("active-user");
    }

    @Test
    @DisplayName("[통합] 포스팅 수 내림차순, 동점 시 member_id 오름차순으로 정렬된다 [success]")
    void getMonthlyRanking_정렬_순서() {
        // Given
        Member first = memberRepository.save(Member.of("user-1", "1번", "TECH"));
        Member second = memberRepository.save(Member.of("user-2", "2번", "TECH"));
        Member third = memberRepository.save(Member.of("user-3", "3번", "TECH"));

        summaryRepository.upsertTotal(first.getId(), YEAR_MONTH, 5);
        summaryRepository.upsertTotal(second.getId(), YEAR_MONTH, 10);
        summaryRepository.upsertTotal(third.getId(), YEAR_MONTH, 10); // second와 동점, member_id로 tie-break

        // When
        MonthlyRankingResponse result = statisticsService.getMonthlyRanking(YEAR_MONTH);

        // Then
        assertThat(result.rankings()).extracting(MonthlyRankingResponse.Entry::nickname)
            .containsExactly("user-2", "user-3", "user-1");
        assertThat(result.rankings()).extracting(MonthlyRankingResponse.Entry::rank)
            .containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("[통합] 상위 10명까지만 반환된다 [success]")
    void getMonthlyRanking_상위10명만_반환() {
        // Given
        for (int i = 1; i <= 12; i++) {
            Member member = memberRepository.save(Member.of("user-" + i, "멤버" + i, "TECH"));
            summaryRepository.upsertTotal(member.getId(), YEAR_MONTH, i);
        }

        // When
        MonthlyRankingResponse result = statisticsService.getMonthlyRanking(YEAR_MONTH);

        // Then
        assertThat(result.rankings()).hasSize(10);
        assertThat(result.rankings().get(0).totalCount()).isEqualTo(12);
        assertThat(result.rankings().get(9).totalCount()).isEqualTo(3);
    }
}
