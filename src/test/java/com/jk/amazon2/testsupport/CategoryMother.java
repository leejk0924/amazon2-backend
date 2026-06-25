package com.jk.amazon2.testsupport;

import com.jk.amazon2.category.dto.CategoryRequest;

public class CategoryMother {

    public static final String INSERT_SQL =
            "INSERT INTO blog_category (code, name, description, created_at, created_by) " +
            "VALUES (?, ?, ?, NOW(), 'system')";

    public static Object[] defaultParams(String code, String name) {
        return new Object[]{code, name, code + " 설명"};
    }

    public static Object[] fullParams(String code, String name, String description) {
        return new Object[]{code, name, description};
    }

    public static CategoryRequest.CategoryCreateDto createDto(String code, String name, String description) {
        return new CategoryRequest.CategoryCreateDto(code, name, description);
    }

    public static CategoryRequest.CategoryUpdateDto updateDto(String name, String description) {
        return CategoryRequest.CategoryUpdateDto.of(name, description);
    }
}
