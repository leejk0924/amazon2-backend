package com.jk.amazon2.posting.service;

import com.jk.amazon2.member.repository.MemberRepository;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
