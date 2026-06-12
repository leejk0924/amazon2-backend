package com.jk.amazon2.posting.dto;

import java.time.LocalDate;

public class PostingRequest {
    public record PostingSearchDto(
            LocalDate startDate
    ) {}
}
