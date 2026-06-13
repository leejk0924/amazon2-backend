package com.jk.amazon2.posting.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ErrorLogDto(
    Long id,
    Long memberId,
    String memberNickname,
    LocalDate targetDate,
    String dayOfWeek,
    String errorMessage,
    Integer retryCount,
    LocalDateTime createdAt
) {}
