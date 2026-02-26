package com.jk.amazon2.service;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.entity.Category;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.RestApiException;
import com.jk.amazon2.repository.CategoryRepository;
import com.jk.amazon2.repository.spec.CategorySpecification;
import com.jk.amazon2.service.dto.CategoryCommand;
import com.jk.amazon2.service.dto.CategoryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResult.Detail create(CategoryCommand.Create command) {
        if (categoryRepository.existsById(command.getCode())) {
            throw new RestApiException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        Category category = Category.of(
                command.getCode(),
                command.getName(),
                command.getDescription()
        );
        return CategoryResult.Detail.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResult.Detail update(CategoryCommand.Update inputCategory) {
        Category category = categoryRepository
                .findById(inputCategory.getCode())
                .orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        category.updateNameCategory(inputCategory.getName());
        category.updateDescription(inputCategory.getDescription());

        return CategoryResult.Detail.from(category);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResult.Info> getCategories(CategoryRequest.CategorySearchCondition condition, Pageable pageable) {
        Specification<Category> spec = CategorySpecification.searchWith(condition);
        return categoryRepository.findAll(spec, pageable).map(CategoryResult.Info::from);
    }
}
