package com.jk.amazon2.controller.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryRequest {

    public record CategoryUpdateDto(
            @NotBlank(message = "카테고리 이름은 필수 입니다.")
            @Size(max = 50, message = "카테고리 이름은 최대 50자까지 입력 가능합니다.")
            String name,
            @Nullable
            @Size(max = 50, message = "카테고리 설명은 최대 50자까지 입력 가능합니다.")
            String description
    ) {
        public static CategoryUpdateDto of(String name, String description) {
            return new CategoryUpdateDto(name, description);
        }
    }

    public record CategoryCreateDto(
            @NotBlank(message = "카테고리 코드는 필수 입니다.")
            @Size(max = 10, message = "카테고리 코드는 최대 10자까지 입력 가능합니다.")
            String code,
            @NotBlank(message = "카테고리 이름은 필수 입니다.")
            @Size(max = 50, message = "카테고리 이름은 최대 50자까지 입력 가능합니다.")
            String name,

            @Nullable
            @Size(max = 50, message = "카테고리 설명은 최대 50자까지 입력 가능합니다.")
            String description
    ) {}

    public record CategorySearchCondition(
            @NotBlank(message = "카테고리 코드는 필수 입니다.")
            @Size(max = 10, message = "카테고리 코드는 최대 10자까지 입력 가능합니다.")
            String code,
            @NotBlank(message = "카테고리 이름은 필수 입니다.")
            @Size(max = 50, message = "카테고리 이름은 최대 50자까지 입력 가능합니다.")
            String name
    ) {
        public static CategorySearchCondition of(String code, String name) {
            return new CategorySearchCondition(code, name);
        }
    }
}
