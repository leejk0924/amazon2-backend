package com.jk.amazon2.category.repository;

import com.jk.amazon2.category.dto.CategoryRequest;
import com.jk.amazon2.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryQueryRepository {
    Page<Category> search(CategoryRequest.CategorySearchCondition condition, Pageable pageable);
}
