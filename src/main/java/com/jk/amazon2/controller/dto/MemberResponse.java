package com.jk.amazon2.controller.dto;

import com.jk.amazon2.service.dto.MemberResult;

import java.time.LocalDate;

public class MemberResponse {
    public record MemberCreateDto(
            Long id,
            String nickname,
            String categoryCode,
            LocalDate createdAt
    ){
        public static MemberCreateDto from(MemberResult.Detail detail) {
            return new MemberCreateDto(
                    detail.getId(),
                    detail.getNickname(),
                    detail.getCategoryCode(),
                    detail.getCreatedAt().toLocalDate()
            );
        }
    }
    
    @Deprecated
    public record MemberDto(
            String nickname,
            String categoryName,
            LocalDate joinDate,
            String status
    ) {}

    public record MemberUpdateDto (
            String nickname,
            String categoryCode
    ){
        public static MemberUpdateDto from(MemberResult.Update update) {
            return new MemberUpdateDto(update.nickname(), update.categoryCode());
        }
    }

    public record MemberListDto(
            String nickname,
            String categoryName,
            LocalDate joinDate,
            String status
    ) {
        public static MemberListDto from(MemberResult.Summary summary) {
            return new MemberListDto(
                    summary.nickname(),
                    summary.categoryName(),
                    summary.createdAt().toLocalDate(),
                    summary.deleted() ? "deleted" : "active"
            );
        }
    }
}
