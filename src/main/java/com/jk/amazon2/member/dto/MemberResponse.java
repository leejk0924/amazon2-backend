package com.jk.amazon2.member.dto;

import com.jk.amazon2.member.dto.MemberResult;

import java.time.LocalDate;

public class MemberResponse {
    public record MemberCreateDto(
            Long id,
            String nickname,
            String name,
            String categoryCode,
            LocalDate createdAt
    ){
        public static MemberCreateDto from(MemberResult.Detail detail) {
            return new MemberCreateDto(
                    detail.getId(),
                    detail.getNickname(),
                    detail.getName(),
                    detail.getCategoryCode(),
                    detail.getCreatedAt().toLocalDate()
            );
        }
    }

    public record MemberUpdateDto (
            String nickname,
            String name,
            String categoryCode
    ){
        public static MemberUpdateDto from(MemberResult.Update update) {
            return new MemberUpdateDto(update.nickname(), update.name(), update.categoryCode());
        }
    }

    public record MemberDetailDto(
            Long id,
            String nickname,
            String name,
            String categoryCode,
            LocalDate joinDate,
            String status
    ) {
        public static MemberDetailDto from(MemberResult.Detail detail) {
            return new MemberDetailDto(
                    detail.getId(),
                    detail.getNickname(),
                    detail.getName(),
                    detail.getCategoryCode(),
                    detail.getCreatedAt().toLocalDate(),
                    detail.isDeleted() ? "deleted" : "active"
            );
        }
    }

    public record MemberListDto(
            String nickname,
            String name,
            String categoryName,
            LocalDate joinDate,
            String status
    ) {
        public static MemberListDto from(MemberResult.Summary summary) {
            return new MemberListDto(
                    summary.nickname(),
                    summary.name(),
                    summary.categoryName(),
                    summary.createdAt().toLocalDate(),
                    summary.deleted() ? "deleted" : "active"
            );
        }
    }
}
