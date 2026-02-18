package com.jk.amazon2.controller.dto;

import java.time.LocalDate;

public class MemberResponse {
    public record MemberDto(
            String nickname,
            String categoryName,
            LocalDate joinDate,
            String status
    ) {}
}
