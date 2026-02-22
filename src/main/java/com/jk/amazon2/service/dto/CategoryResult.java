package com.jk.amazon2.service.dto;

import com.jk.amazon2.entity.Category;

import java.time.LocalDateTime;

public class CategoryResult {
    public record Detail(
            String code,
            String name,
            String description,
            LocalDateTime createdAt,
            String createdBy) {
        public static Detail from(Category entity) {
            return new Detail(
                    entity.getCode(),
                    entity.getName(),
                    entity.getDescription(),
                    entity.getCreatedAt(),
                    entity.getCreatedBy()
            );
        }
    }
}
