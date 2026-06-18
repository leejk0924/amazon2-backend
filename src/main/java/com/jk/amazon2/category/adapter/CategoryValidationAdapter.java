package com.jk.amazon2.category.adapter;

import com.jk.amazon2.category.exception.CategoryErrorCode;
import com.jk.amazon2.category.repository.CategoryRepository;
import com.jk.amazon2.common.exception.RestApiException;
import com.jk.amazon2.common.port.CategoryValidationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryValidationAdapter implements CategoryValidationPort {

	private final CategoryRepository categoryRepository;

	@Override
	public void validateCategoryExists(String categoryCode) {
		categoryRepository
				.findByCodeAndDeletedFalse(categoryCode)
				.orElseThrow(() -> new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));
	}
}
