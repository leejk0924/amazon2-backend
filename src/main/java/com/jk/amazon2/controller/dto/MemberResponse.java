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
}
