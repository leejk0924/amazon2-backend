package com.jk.amazon2.entity;

import com.jk.amazon2.common.entity.BaseCreation;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "blog_category")
@Entity
public class Category extends BaseCreation implements Persistable<String> {
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

    public void updateNameCategory(String name) {
        if (!Objects.equals(this.name, name)) {
            this.name = name;
        }
    }

    public void updateDescription(String description) {
        if(!Objects.equals(this.description, description)) {
            this.description = description;
        }
    }

    @Override
    public @Nullable String getId() {
        return this.code;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
