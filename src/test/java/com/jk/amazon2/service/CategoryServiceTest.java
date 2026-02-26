package com.jk.amazon2.service;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.entity.Category;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.repository.CategoryRepository;
import com.jk.amazon2.service.dto.CategoryCommand;
import com.jk.amazon2.service.dto.CategoryResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private CategoryService categoryService;

    @Nested
    @DisplayName("Category 생성 - 단위 테스트")
    class CreateCategory{
        @DisplayName("카테고리 생성 시, 입력된 정보가 엔티티로 변환되어 저장 [success]")
        @Test
        void category_create_success_test() {
            // given
            String code = "TECH";
            String name = "Technology";
            String description = "Electronic devices and gadgets";

            var inputCategory = CategoryCommand.Create.of(code, name, description);

            given(categoryRepository.save(any(Category.class)))
                    .will(AdditionalAnswers.returnsFirstArg());

            // when
            CategoryResult.Detail savedCategory = categoryService.create(inputCategory);

            // then
            // 1. 반환값 검증
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

            // 2. Repository에 전달된 실제 엔티티 캡처 및 검증
            ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(categoryCaptor.capture());
            Category capturedCategory = categoryCaptor.getValue();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedCategory.getCode()).isEqualTo(code);
                softly.assertThat(capturedCategory.getName()).isEqualTo(name);
                softly.assertThat(capturedCategory.getDescription()).isEqualTo(description);
            });
        }

        @DisplayName("카테고리 생성 시, 이미 존재하는 카테고리인 경우 [fail]")
        @Test
        void category_create_fail_test() {
            // given
            String code = "TECH";
            String name = "Technology";
            String description = "Electronic devices and gadgets";

            CategoryCommand.Create inputCategory = CategoryCommand.Create.of(code, name, description);

            given((categoryRepository.existsById(eq(code))))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> categoryService.create(inputCategory))
                    .extracting("errorCode")
                    .isEqualTo(CategoryErrorCode.CATEGORY_ALREADY_EXISTS);

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

    @Nested
    @DisplayName("Category 조회 - 단위 테스트")
    class ReadCategory {

        @Captor
        ArgumentCaptor<Specification<Category>> specCaptor;

        @DisplayName("검색 조건으로 카테고리 목록 페이징 조회 성공")
        @Test
        void searchCategories_Success_WithCondition() {
            // given
            Category category1 = Category.of("TECH", "Technology", "Desc1");
            Category category2 = Category.of("FASHION", "Fashion", "Desc2");
            List<Category> categories = List.of(category1, category2);

            var condition = CategoryRequest.CategorySearchCondition.of(null, "Tech");
            Pageable pageable = PageRequest.of(0, 10);

            Page<Category> categoryPage = new PageImpl<>(categories, pageable, categories.size());
            given(categoryRepository.findAll(ArgumentMatchers.<Specification<Category>>any(), any(Pageable.class)))
                    .willReturn(categoryPage);

            // when
            Page<CategoryResult.Info> resultPage = categoryService.getCategories(condition, pageable);

            // then
            SoftAssertions.assertSoftly(softly -> {
                // 1. 페이징 메타데이터 검증
                softly.assertThat(resultPage.getTotalElements()).as("전체 요소 수").isEqualTo(2);
                softly.assertThat(resultPage.getTotalPages()).as("전체 페이지 수").isEqualTo(1);
                softly.assertThat(resultPage.getNumber()).as("현재 페이지 번호").isEqualTo(0);
                softly.assertThat(resultPage.getSize()).as("페이지 크기").isEqualTo(10);

                // 2. 컨텐츠 내용 및 순서 검증
                softly.assertThat(resultPage.getContent())
                        .extracting(CategoryResult.Info::getCode, CategoryResult.Info::getName, CategoryResult.Info::getDescription)
                        .containsExactly(
                                tuple("TECH", "Technology", "Desc1"),
                                tuple("FASHION", "Fashion", "Desc2")
                        );
            });

            verify(categoryRepository, times(1)).findAll(ArgumentMatchers.<Specification<Category>>any(), any(Pageable.class));
            verify(categoryRepository).findAll(specCaptor.capture(), eq(pageable));

            Specification<Category> capturedSpec = specCaptor.getValue();
            assertThat(capturedSpec).isNotNull();
        }

        @DisplayName("검색 결과가 없을 경우 빈 페이지 반환")
        @Test
        void searchCategories_Success_EmptyResult() {
            // given
            Pageable pageable = PageRequest.of(0, 10);

            given(categoryRepository.findAll(ArgumentMatchers.<Specification<Category>>any(), any(Pageable.class)))
                    .willReturn(Page.empty(pageable));

            // when
            Page<CategoryResult.Info> resultPage = categoryService.getCategories(null, pageable);

            // then
            assertThat(resultPage).as("결과 페이지 객체").isNotNull();

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(resultPage.getContent()).as("결과 목록").isEmpty();
                softly.assertThat(resultPage.getTotalElements()).as("전체 요소 수").isZero();
                softly.assertThat(resultPage.getTotalPages()).as("전체 페이지 수").isZero();
            });
        }
    }
}