package com.jk.amazon2.posting.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record StatisticsResponse(
    LocalDate startDate,
    LocalDate endDate,
    Integer totalPostings,
    List<UserStatistics> users
) {
    public record UserStatistics(
        Long memberId,
        String nickname,
        Integer totalPostings,
        Map<String, Integer> byDayOfWeek
    ) {}
}
