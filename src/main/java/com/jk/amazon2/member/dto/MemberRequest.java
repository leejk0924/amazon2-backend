package com.jk.amazon2.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MemberRequest {
    public record MemberDto(
            String nickname,
            String name,
            String categoryCode
    ) {}

    public record MemberSearchCondition(
            String nickname,
            String categoryCode,
            String status
    ) {}

    public record MemberCreateDto(
            @NotBlank(message = "닉네임은 필수 입니다.")
            @Size(max = 50, message = "닉네임은 최대 50자까지 입력 가능합니다.")
            String nickname,
            @NotBlank(message = "이름은 필수 입니다.")
            @Size(max = 20, message = "이름은 최대 20자까지 입력 가능합니다.")
            String name,
            @NotBlank(message = "카테고리 코드는 필수 입니다.")
            String categoryCode
    ) {
        public static MemberCreateDto of(String nickname, String name, String categoryCode) {
            return new MemberCreateDto(nickname, name, categoryCode);
        }
    }
}
