package com.jk.amazon2.category.adapter;

import com.jk.amazon2.category.entity.Category;
import com.jk.amazon2.category.exception.CategoryErrorCode;
import com.jk.amazon2.category.repository.CategoryRepository;
import com.jk.amazon2.common.exception.RestApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryValidationAdapter 단위 테스트")
class CategoryValidationAdapterTest {

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private CategoryValidationAdapter categoryValidationAdapter;

	@Test
	@DisplayName("카테고리가 존재하면 검증을 통과한다")
	void validateCategoryExists_Success() {
		// Given
		String categoryCode = "TRIP";
		Category category = Category.of("여행", categoryCode, "여행 카테고리");
		given(categoryRepository.findByCodeAndDeletedFalse(categoryCode))
				.willReturn(Optional.of(category));

		// When
		categoryValidationAdapter.validateCategoryExists(categoryCode);

		// Then
		verify(categoryRepository, times(1)).findByCodeAndDeletedFalse(categoryCode);
	}

	@Test
	@DisplayName("카테고리가 존재하지 않으면 예외를 던진다")
	void validateCategoryExists_NotFound() {
		// Given
		String categoryCode = "INVALID_CODE";
		given(categoryRepository.findByCodeAndDeletedFalse(anyString()))
				.willReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> categoryValidationAdapter.validateCategoryExists(categoryCode))
				.isInstanceOf(RestApiException.class)
				.hasMessageContaining(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage());
	}
}
