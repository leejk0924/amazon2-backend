package com.jk.amazon2.service;

import com.jk.amazon2.entity.Category;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.RestApiException;
import com.jk.amazon2.repository.CategoryRepository;
import com.jk.amazon2.service.dto.CategoryCommand;
import com.jk.amazon2.service.dto.CategoryResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResult.Detail create(CategoryCommand.Create command) {
        if (categoryRepository.existsById(command.code())) {
            throw new RestApiException(CategoryErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        Category category = Category.of(
                command.code(),
                command.name(),
                command.description()
        );
        return CategoryResult.Detail.from(categoryRepository.save(category));
    }
}
