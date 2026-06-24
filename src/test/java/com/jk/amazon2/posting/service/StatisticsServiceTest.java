package com.jk.amazon2.posting.service;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.dto.StatisticsResponse;
import com.jk.amazon2.posting.dto.WeeklyStatisticsResponse;
import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.exception.PostingException;
import com.jk.amazon2.posting.repository.PostingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private PostingRepository postingRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private Posting createPosting(Long memberId, LocalDate weekStart,
                                  int mon, int tue, int wed, int thu, int fri, int sat, int sun) {
        return new Posting(memberId, weekStart, mon, tue, wed, thu, fri, sat, sun, "test");
    }

    private Member createMember(Long id, String nickname) {
        Member member = Member.of(nickname, nickname, null);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    // ==================== getStatistics ====================

    @Test
    @DisplayName("기간 통계 - 멤버별 포스팅 수와 요일별 집계 정상 동작")
    void getStatistics_멤버별_집계_정상동작() {
        // Given
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        Member memberA = createMember(1L, "alice");
        Member memberB = createMember(2L, "bob");

        LocalDate weekStart = LocalDate.of(2026, 6, 2);
        when(memberRepository.findAllByDeletedFalse()).thenReturn(List.of(memberA, memberB));
        when(postingRepository.findAllByMemberIdsAndDateRange(List.of(1L, 2L), start, end)).thenReturn(List.of(
            createPosting(1L, weekStart, 3, 2, 1, 0, 0, 0, 0),  // alice: 6개
            createPosting(2L, weekStart, 1, 1, 0, 0, 0, 0, 0)   // bob: 2개
        ));

        // When
        StatisticsResponse result = statisticsService.getStatistics(start, end);

        // Then
        assertThat(result.totalPostings()).isEqualTo(8);
        assertThat(result.users()).hasSize(2);

        StatisticsResponse.UserStatistics aliceStats = result.users().stream()
            .filter(u -> u.nickname().equals("alice")).findFirst().orElseThrow();
        assertThat(aliceStats.totalPostings()).isEqualTo(6);
        assertThat(aliceStats.byDayOfWeek().get("mon")).isEqualTo(3);
        assertThat(aliceStats.byDayOfWeek().get("tue")).isEqualTo(2);
    }

    @Test
    @DisplayName("기간 통계 - 포스팅 없는 멤버는 결과에서 제외")
    void getStatistics_포스팅없는_멤버_제외() {
        // Given
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        Member memberA = createMember(1L, "alice");
        Member memberB = createMember(2L, "bob");

        when(memberRepository.findAllByDeletedFalse()).thenReturn(List.of(memberA, memberB));
        when(postingRepository.findAllByMemberIdsAndDateRange(any(), eq(start), eq(end))).thenReturn(List.of(
            createPosting(1L, LocalDate.of(2026, 6, 2), 1, 0, 0, 0, 0, 0, 0)
        ));

        // When
        StatisticsResponse result = statisticsService.getStatistics(start, end);

        // Then: bob은 포스팅이 없으므로 제외
        assertThat(result.users()).hasSize(1);
        assertThat(result.users().get(0).nickname()).isEqualTo("alice");
    }

    @Test
    @DisplayName("기간 통계 - null 요일 값은 0으로 처리 (NPE 방어)")
    void getStatistics_null요일값_0으로처리() {
        // Given
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        Member member = createMember(1L, "alice");
        Posting postingWithNulls = new Posting(1L, LocalDate.of(2026, 6, 2), null, null, 2, null, null, null, null, "test");

        when(memberRepository.findAllByDeletedFalse()).thenReturn(List.of(member));
        when(postingRepository.findAllByMemberIdsAndDateRange(any(), eq(start), eq(end)))
            .thenReturn(List.of(postingWithNulls));

        // When: NPE 발생하지 않아야 함
        StatisticsResponse result = statisticsService.getStatistics(start, end);

        // Then
        assertThat(result.users()).hasSize(1);
        assertThat(result.users().get(0).totalPostings()).isEqualTo(2);
        assertThat(result.users().get(0).byDayOfWeek().get("wed")).isEqualTo(2);
        assertThat(result.users().get(0).byDayOfWeek().get("mon")).isEqualTo(0);
    }

    @Test
    @DisplayName("기간 통계 - 활성 멤버가 없으면 빈 결과 반환")
    void getStatistics_활성멤버없으면_빈결과() {
        // Given
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        when(memberRepository.findAllByDeletedFalse()).thenReturn(List.of());

        // When
        StatisticsResponse result = statisticsService.getStatistics(start, end);

        // Then: DB 쿼리 없이 즉시 반환
        assertThat(result.totalPostings()).isEqualTo(0);
        assertThat(result.users()).isEmpty();
        verify(postingRepository, org.mockito.Mockito.never())
            .findAllByMemberIdsAndDateRange(any(), any(), any());
    }

    @Test
    @DisplayName("기간 통계 - DB에 올바른 memberIds와 날짜 범위로 쿼리")
    void getStatistics_올바른_파라미터로_쿼리() {
        // Given
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        Member memberA = createMember(10L, "alice");
        Member memberB = createMember(20L, "bob");

        when(memberRepository.findAllByDeletedFalse()).thenReturn(List.of(memberA, memberB));
        when(postingRepository.findAllByMemberIdsAndDateRange(any(), eq(start), eq(end)))
            .thenReturn(List.of());

        // When
        statisticsService.getStatistics(start, end);

        // Then
        verify(postingRepository).findAllByMemberIdsAndDateRange(List.of(10L, 20L), start, end);
    }

    @Test
    @DisplayName("주간 통계 - 활동 인원과 총 포스팅 수 집계")
    void getWeeklyStatistics_활동인원_포스팅수_정상집계() {
        // Given
        LocalDate weekStart = LocalDate.of(2026, 6, 15); // 월요일
        when(memberRepository.countByDeletedFalse()).thenReturn(10L);
        when(postingRepository.findAllByWeekStartDate(weekStart)).thenReturn(List.of(
            createPosting(1L, weekStart, 2, 1, 0, 0, 0, 0, 0),  // 3개
            createPosting(2L, weekStart, 1, 1, 1, 0, 0, 0, 0),  // 3개
            createPosting(3L, weekStart, 0, 0, 0, 0, 0, 0, 0)   // 0개 (비활동)
        ));

        // When
        WeeklyStatisticsResponse result = statisticsService.getWeeklyStatistics(weekStart);

        // Then
        assertThat(result.weekStartDate()).isEqualTo(weekStart);
        assertThat(result.totalMemberCount()).isEqualTo(10L);
        assertThat(result.activeMemberCount()).isEqualTo(2L);
        assertThat(result.totalPostingCount()).isEqualTo(6L);
        assertThat(result.averagePostingPerActiveMember()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("주간 통계 - 해당 주 포스팅이 없으면 평균은 0.0")
    void getWeeklyStatistics_포스팅없으면_평균0() {
        // Given
        LocalDate weekStart = LocalDate.of(2026, 6, 15);
        when(memberRepository.countByDeletedFalse()).thenReturn(5L);
        when(postingRepository.findAllByWeekStartDate(weekStart)).thenReturn(List.of());

        // When
        WeeklyStatisticsResponse result = statisticsService.getWeeklyStatistics(weekStart);

        // Then
        assertThat(result.totalPostingCount()).isEqualTo(0L);
        assertThat(result.activeMemberCount()).isEqualTo(0L);
        assertThat(result.averagePostingPerActiveMember()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("주간 통계 - 1인 평균 포스팅 수 소수점 2자리 반올림")
    void getWeeklyStatistics_평균_소수점2자리() {
        // Given
        LocalDate weekStart = LocalDate.of(2026, 6, 15);
        when(memberRepository.countByDeletedFalse()).thenReturn(10L);
        when(postingRepository.findAllByWeekStartDate(weekStart)).thenReturn(List.of(
            createPosting(1L, weekStart, 1, 0, 0, 0, 0, 0, 0),
            createPosting(2L, weekStart, 1, 0, 0, 0, 0, 0, 0),
            createPosting(3L, weekStart, 1, 0, 0, 0, 0, 0, 0)
        ));

        // When
        WeeklyStatisticsResponse result = statisticsService.getWeeklyStatistics(weekStart);

        // Then: 3 / 3 = 1.0
        assertThat(result.totalPostingCount()).isEqualTo(3L);
        assertThat(result.activeMemberCount()).isEqualTo(3L);
        assertThat(result.averagePostingPerActiveMember()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("주간 통계 - null 포스팅 값은 0으로 처리")
    void getWeeklyStatistics_null값_0으로처리() {
        // Given
        LocalDate weekStart = LocalDate.of(2026, 6, 15);
        when(memberRepository.countByDeletedFalse()).thenReturn(3L);

        Posting postingWithNulls = new Posting(1L, weekStart, null, null, null, null, null, null, null, "test");
        when(postingRepository.findAllByWeekStartDate(weekStart)).thenReturn(List.of(postingWithNulls));

        // When
        WeeklyStatisticsResponse result = statisticsService.getWeeklyStatistics(weekStart);

        // Then: null 포스팅은 비활동 처리
        assertThat(result.activeMemberCount()).isEqualTo(0L);
        assertThat(result.totalPostingCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("주간 통계 - totalMemberCount는 삭제되지 않은 전체 회원 수")
    void getWeeklyStatistics_총인원은_비삭제회원수() {
        // Given
        LocalDate weekStart = LocalDate.of(2026, 6, 15);
        when(memberRepository.countByDeletedFalse()).thenReturn(7L);
        when(postingRepository.findAllByWeekStartDate(weekStart)).thenReturn(List.of(
            createPosting(1L, weekStart, 1, 0, 0, 0, 0, 0, 0)
        ));

        // When
        WeeklyStatisticsResponse result = statisticsService.getWeeklyStatistics(weekStart);

        // Then
        assertThat(result.totalMemberCount()).isEqualTo(7L);
        assertThat(result.activeMemberCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("주간 통계 - 월요일이 아닌 날짜 입력 시 예외 발생")
    void getWeeklyStatistics_월요일아닌날짜_예외발생() {
        // Given
        LocalDate tuesday = LocalDate.of(2026, 6, 16); // 화요일

        // When & Then
        assertThatThrownBy(() -> statisticsService.getWeeklyStatistics(tuesday))
            .isInstanceOf(PostingException.class)
            .hasMessage("주간 시작일은 월요일이어야 합니다");
    }
}
