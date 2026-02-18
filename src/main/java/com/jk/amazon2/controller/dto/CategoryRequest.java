package com.jk.amazon2.controller.dto;

public class CategoryRequest {
    public record CategoryDto(
            String code,
            String name
    ) {}

    public record CategorySearchCondition(
            String code,
            String name
    ) {
    }
}
