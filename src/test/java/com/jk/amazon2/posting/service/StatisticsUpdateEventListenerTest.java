package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.event.StatisticsUpdateEvent;
import com.jk.amazon2.posting.repository.MonthlyPostingSummaryRepository;
import com.jk.amazon2.posting.repository.PostingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsUpdateEventListenerTest {

    @Mock
    private PostingRepository postingRepository;

    @Mock
    private MonthlyPostingSummaryRepository summaryRepository;

    @InjectMocks
    private StatisticsUpdateEventListener listener;

    @Test
    @DisplayName("이벤트를 받으면 weekStartDate가 속한 달 전체를 재합산해서 upsert한다")
    void handle_해당_월_전체를_재합산() {
        // Given
        Long memberId = 1L;
        LocalDate weekStartDate = LocalDate.of(2026, 6, 22);
        LocalDate monthStart = LocalDate.of(2026, 6, 1);
        LocalDate monthEnd = LocalDate.of(2026, 6, 30);
        when(postingRepository.sumTotalByMemberAndMonth(memberId, monthStart, monthEnd)).thenReturn(15);

        // When
        listener.handle(new StatisticsUpdateEvent(memberId, weekStartDate));

        // Then
        verify(postingRepository).sumTotalByMemberAndMonth(memberId, monthStart, monthEnd);
        verify(summaryRepository).upsertTotal(memberId, monthStart, 15);
    }

    @Test
    @DisplayName("12월처럼 월 마지막 날짜 계산이 연도를 넘어가도 정확히 처리한다")
    void handle_12월_연말_경계_처리() {
        // Given
        Long memberId = 2L;
        LocalDate weekStartDate = LocalDate.of(2026, 12, 28);
        LocalDate monthStart = LocalDate.of(2026, 12, 1);
        LocalDate monthEnd = LocalDate.of(2026, 12, 31);
        when(postingRepository.sumTotalByMemberAndMonth(memberId, monthStart, monthEnd)).thenReturn(3);

        // When
        listener.handle(new StatisticsUpdateEvent(memberId, weekStartDate));

        // Then
        verify(summaryRepository).upsertTotal(memberId, monthStart, 3);
    }
}
