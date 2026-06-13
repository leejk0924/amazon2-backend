package com.jk.amazon2.posting.dto;

import java.time.LocalDate;

public record BatchRequest(
    LocalDate startDate,
    LocalDate endDate
) {}
