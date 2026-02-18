package com.jk.amazon2.controller.dto;

public class MemberRequest {
    public record MemberDto(
            String nickname,
            String categoryCode
    ) {}

    public record MemberSearchCondition(
            String nickname,
            String category,
            String status
    ) {}
}
