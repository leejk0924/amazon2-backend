package com.jk.amazon2.controller.dto;

public class CategoryResponse {
    public record CategoryDto(
            String code,
            String name
    ) {}
}
