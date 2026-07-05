package com.jk.amazon2.posting.dto;

import java.time.LocalDate;
import java.util.List;

public record MonthlyRankingResponse(
    LocalDate yearMonth,
    List<Entry> rankings
) {
    public record Entry(
        int rank,
        Long memberId,
        String nickname,
        int totalCount
    ) {}
}
