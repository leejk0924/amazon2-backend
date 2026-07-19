package com.jk.amazon2.posting.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BatchCollectionTimeResponse(
    LocalDate weekStartDate,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {}
