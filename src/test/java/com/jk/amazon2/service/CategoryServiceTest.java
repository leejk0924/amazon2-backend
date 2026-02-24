package com.jk.amazon2.service;

import com.jk.amazon2.entity.Category;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.RestApiException;
import com.jk.amazon2.repository.CategoryRepository;
import com.jk.amazon2.service.dto.CategoryCommand;
import com.jk.amazon2.service.dto.CategoryResult;
import net.datafaker.Faker;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private CategoryService categoryService;

    private final Faker faker = new Faker(Locale.of("ko"));

    @Nested
    @DisplayName("Category 생성 - 단위 테스트")
    class CreateCategory{
        @DisplayName("카테고리 생성 시, 입력된 정보가 엔티티로 변환되어 저장 [success]")
        @Test
        void category_create_success_test() throws Exception {
            // given
            String code = faker.regexify("[A-Z]{5,10}");
            String name = faker.company().industry();
            String description = faker.lorem().sentence();

            var inputCategory = CategoryCommand.Create.of(code, name, description);

            var mockCategory = Category.of(code, name, description);
            given(categoryRepository.save(any(Category.class)))
                    .willReturn(mockCategory);

            // when
            CategoryResult.Detail savedCategory = categoryService.create(inputCategory);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedCategory.getCode())
                        .as("code 검증")
                        .isEqualTo(code);
                softly.assertThat(savedCategory.getName())
                        .as("name 검증")
                        .isEqualTo(name);
                softly.assertThat(savedCategory.getDescription())
                        .as("description 검증")
                        .isEqualTo(description);
            });
        }

        @DisplayName("카테고리 생성 시, 이미 존재하는 카테고리인 경우 [fail]")
        @Test
        void category_create_fail_test() throws Exception {
            // given
            String code = faker.regexify("[A-Z]{5,10}");
            String name = faker.company().industry();
            String description = faker.lorem().sentence();

            CategoryCommand.Create inputCategory = CategoryCommand.Create.of(code, name, description);

            given((categoryRepository.existsById(eq(code))))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> categoryService.create(inputCategory))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(CategoryErrorCode.CATEGORY_ALREADY_EXISTS.getMessage());

            verify(categoryRepository, never()).save(any(Category.class));
        }
    }
    @Nested
    @DisplayName("Category 수정 - 단위 테스트")
    class UpdateCategory{
        @DisplayName("카테고리 수정 시, 입력받은 정보를 기준으로 기존의 데이터를 변환하여 저장 [success]")
        @Test
        void category_update_success_test() {
            // given
            String code = "TECH";
            String initName = "테크";
            String updateName = "전자기기";
            String description = "전자기기 등";

            Category initCategory = Category.of(code, initName, description);
            given(categoryRepository.findById(anyString())
            ).willReturn(Optional.of(initCategory));

            CategoryCommand.Update inputCategory = CategoryCommand.Update.of(code, updateName, description);

            // when
            CategoryResult.Detail updatedCategory = categoryService.update(inputCategory);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(updatedCategory.getCode()).as("code 검증").isEqualTo(code);
                softly.assertThat(updatedCategory.getName()).as("name 변경 검증").isEqualTo(updateName);
            });
        }

        @DisplayName("카테고리 수정 시, 존재하지 않는 카테고리인 경우 [fail]")
        @Test
        void category_update_notFound_fail_test() {
            // given
            CategoryCommand.Update inputCategory = CategoryCommand.Update.of("NOTEXIST", "전자기기", "설명");

            given(categoryRepository.findById(anyString()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> categoryService.update(inputCategory))
                    .extracting("errorCode")
                    .isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
    }
}