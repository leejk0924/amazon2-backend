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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private boolean deleted = Boolean.FALSE;

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

    public void delete() {
        this.deleted = true;
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String prefix = "del_" + now + "_";

        String newName = prefix + this.name;
        this.name = newName.length() > 50 ? newName.substring(0, 50) : newName;
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
