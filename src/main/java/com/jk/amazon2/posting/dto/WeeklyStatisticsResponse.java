package com.jk.amazon2.posting.dto;

import java.time.LocalDate;

public record WeeklyStatisticsResponse(
    LocalDate weekStartDate,
    long totalPostingCount,
    long totalMemberCount,
    long activeMemberCount,
    double averagePostingPerActiveMember
) {}
