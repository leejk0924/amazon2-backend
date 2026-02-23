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
            return new CategoryCreateDto(detail.getCode(), detail.getName(), detail.getDescription());
        }
    }

    @Deprecated
    public record CategoryDto(
            String code,
            String name
    ) {}
}
