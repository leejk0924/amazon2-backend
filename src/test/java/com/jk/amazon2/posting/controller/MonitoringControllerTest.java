package com.jk.amazon2.posting.controller;

import com.jk.amazon2.posting.dto.MonthlyRankingResponse;
import com.jk.amazon2.posting.service.MonitoringService;
import com.jk.amazon2.posting.service.StatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MonitoringControllerTest {

    @Mock
    private MonitoringService monitoringService;

    @Mock
    private StatisticsService statisticsService;

    private MonitoringController monitoringController;

    @BeforeEach
    void setUp() {
        monitoringController = new MonitoringController(monitoringService, statisticsService);
    }

    @DisplayName("GET /api/postings/monthly-ranking - 월별 랭킹 조회 성공 [200 OK]")
    @Test
    void getMonthlyRanking_Success() {
        // given
        LocalDate yearMonth = LocalDate.of(2026, 6, 1);
        MonthlyRankingResponse response = new MonthlyRankingResponse(
            yearMonth,
            List.of(new MonthlyRankingResponse.Entry(1, 1L, "alice", 20))
        );
        given(statisticsService.getMonthlyRanking(yearMonth)).willReturn(response);

        // when
        ResponseEntity<MonthlyRankingResponse> result = monitoringController.getMonthlyRanking(yearMonth);

        // then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
    }
}
