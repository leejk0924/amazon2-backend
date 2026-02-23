package com.jk.amazon2.service.dto;

import com.jk.amazon2.entity.Category;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryResult {
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Detail {
        private String code;
        private String name;
        private String description;
        private LocalDateTime createdAt;
        private String createdBy;

        private Detail(
                String code,
                String name,
                String description,
                LocalDateTime createdAt,
                String createdBy
        ) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.createdAt = createdAt;
            this.createdBy = createdBy;
        }

        public static Detail of(
                String code,
                String name,
                String description,
                LocalDateTime createdAt,
                String createdBy
        ) {
            return new Detail(code, name, description, createdAt, createdBy);
        }

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
