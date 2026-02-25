package com.jk.amazon2.controller.dto;

import com.jk.amazon2.service.dto.CategoryResult;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryResponse {
    public record CategoryCreateDto(
            String categoryCode,
            String categoryName,
            String description
    ) {
        public static CategoryCreateDto from(CategoryResult.Detail detail) {
            return new CategoryCreateDto(detail.getCode(), detail.getName(), detail.getDescription());
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class CategoryUpdateDto {
        private String categoryCode;
        private String categoryName;
        private String description;
        public static CategoryUpdateDto from(CategoryResult.Detail detail) {
            CategoryUpdateDto dto = new CategoryUpdateDto();
            dto.categoryCode = detail.getCode();
            dto.categoryName = detail.getName();
            dto.description = detail.getDescription();
            return dto;
        }
    }

    @Deprecated
    public record CategoryDto(
            String code,
            String name
    ) {}
}
