package com.jk.amazon2.controller.dto;

import java.time.LocalDate;

public class PostingRequest {
    public record PostingSearchDto(
            LocalDate startDate
    ) {}
}
