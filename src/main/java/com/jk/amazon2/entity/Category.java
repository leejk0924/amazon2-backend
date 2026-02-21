package com.jk.amazon2.entity;

import com.jk.amazon2.common.entity.BaseCreation;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "blog_category")
@Entity
public class Category extends BaseCreation {
    @Id
    private String code;
    private String name;
    private String description;

    public static Category of(String code, String name, String description) {
        Category category = new Category();
        category.code = code;
        category.name = name;
        category.description = description;
        return category;
    }
}
