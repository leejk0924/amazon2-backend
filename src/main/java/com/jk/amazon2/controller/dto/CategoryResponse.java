package com.jk.amazon2.controller.dto;

import com.jk.amazon2.service.dto.CategoryResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryResponse {
    public record CategoryCreateDto(
            String categoryCode,
            String categoryName,
            String description
    ) {
        public static CategoryCreateDto from(CategoryResult.Detail detail) {
            return new CategoryCreateDto(detail.code(), detail.name(), detail.description());
        }
    }

    @Deprecated
    public record CategoryDto(
            String code,
            String name
    ) {}
}
