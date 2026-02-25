package com.jk.amazon2.controller;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.controller.dto.CategoryResponse;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.RestApiException;
import com.jk.amazon2.service.CategoryService;
import com.jk.amazon2.service.dto.CategoryCommand;
import com.jk.amazon2.service.dto.CategoryResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    private CategoryController categoryController;

    @BeforeEach
    void setUp() {
        categoryController = new CategoryController(categoryService);
    }

    @DisplayName("Category 생성 - 단위 테스트")
    @Nested
    class CreateCategory {

        @DisplayName("POST /categories - 카테고리 생성 성공 [201 Created]")
        @Test
        void createCategory_Success() {
            // given
            String code = "TECH";
            String name = "Technology";
            String description = "Electronic devices and gadgets";

            var requestDto = new CategoryRequest.CategoryCreateDto(code, name, description);

            CategoryResult.Detail mockResult = CategoryResult.Detail.of(
                    code, name, description,
                    LocalDateTime.now(), "admin"
            );

            given(categoryService.create(any(CategoryCommand.Create.class)))
                    .willReturn(mockResult);

            // when
            ResponseEntity<CategoryResponse.CategoryCreateDto> response =
                    categoryController.createCategory(requestDto);

            // then (ResponseEntity 검증)
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(response.getStatusCode()).as("상태코드 검증").isEqualTo(HttpStatus.CREATED);
                softly.assertThat(Objects.requireNonNull(response.getBody()))
                        .as("응답 본문 검증")
                        .isNotNull()
                        .satisfies(body -> {
                            softly.assertThat(response.getBody().categoryCode()).as("카테고리 코드 검증").isEqualTo(code);
                            softly.assertThat(response.getBody().categoryName()).as("카테고리 이름 검증").isEqualTo(name);
                            softly.assertThat(response.getBody().description()).as("카테고리 설명 검증").isEqualTo(description);
                        });
            });

            // Verify Service Call
            ArgumentCaptor<CategoryCommand.Create> commandCaptor = ArgumentCaptor.forClass(CategoryCommand.Create.class);
            verify(categoryService).create(commandCaptor.capture());

            CategoryCommand.Create capturedCommand = commandCaptor.getValue();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedCommand.getCode()).as("DTO의 code가 Command의 code 검증").isEqualTo(code);
                softly.assertThat(capturedCommand.getName()).as("DTO의 name이 Command로 정확히 매핑되었는가").isEqualTo(name);
                softly.assertThat(capturedCommand.getDescription()).as("DTO의 description이 Command로 정확히 매핑되었는가").isEqualTo(description);
            });
        }

        @DisplayName("POST /categories - 유효하지 않은 입력값 검증")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideInvalidCategoryRequests")
        void createCategory_Fail_Validation(
                String testCase,
                String code,
                String name,
                String description,
                String expectedMessage
        ) {
            // given
            var requestDto = new CategoryRequest.CategoryCreateDto(code, name, description);

            var factory = Validation.buildDefaultValidatorFactory();
            var validator = factory.getValidator();

            // when
            Set<ConstraintViolation<CategoryRequest.CategoryCreateDto>> violations = validator.validate(requestDto);

            // then
            assertThat(violations).isNotEmpty();
            assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains(expectedMessage);
        }

        static Stream<Arguments> provideInvalidCategoryRequests() {
            return Stream.of(
                    Arguments.of("코드가 blank인 경우", "", "Name", "Desc", "카테고리 코드는 필수 입니다."),
                    Arguments.of("코드가 null인 경우", null, "Name", "Desc", "카테고리 코드는 필수 입니다."),
                    Arguments.of("코드가 10자 초과인 경우", "A".repeat(11), "Name", "Desc", "카테고리 코드는 최대 10자까지 입력 가능합니다."),
                    Arguments.of("이름이 null인 경우", "CODE", null, "Desc", "카테고리 이름은 필수 입니다."),
                    Arguments.of("이름이 blank인 경우", "CODE", "", "Desc", "카테고리 이름은 필수 입니다."),
                    Arguments.of("이름이 50자 초과인 경우", "CODE", "A".repeat(51), "Desc", "카테고리 이름은 최대 50자까지 입력 가능합니다.")
            );
        }
    }

    @Nested
    @DisplayName("Category 수정 - 단위 테스트")
    class UpdateCategory {

        @DisplayName("PUT /categories/{code} - 카테고리 수정 성공 [200 OK]")
        @Test
        void updateCategory_Success() {
            // given
            String code = "TECH";
            String updateName = "Updated Name";
            String description = "Updated Description";

            var requestDto = CategoryRequest.CategoryUpdateDto.of(updateName, description);

            CategoryResult.Detail mockResult = CategoryResult.Detail.of(
                    code, updateName, description,
                    LocalDateTime.now(), "admin"
            );

            given(categoryService.update(any(CategoryCommand.Update.class)))
                    .willReturn(mockResult);

            // when
            ResponseEntity<CategoryResponse.CategoryUpdateDto> response =
                    categoryController.updateCategory(code, requestDto);

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            // 2. 응답 본문 검증
            CategoryResponse.CategoryUpdateDto responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
            assertThat(responseBody.getCategoryCode()).isEqualTo(code);
            assertThat(responseBody.getCategoryName()).isEqualTo(updateName);
            assertThat(responseBody.getDescription()).isEqualTo(description);

            // 3.서비스 호출 검증 (서비스로 전달된 데이터 검증)
            ArgumentCaptor<CategoryCommand.Update> captor = ArgumentCaptor.forClass(CategoryCommand.Update.class);
            verify(categoryService).update(captor.capture());

            CategoryCommand.Update captured = captor.getValue();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(captured.getCode()).isEqualTo(code);
                softly.assertThat(captured.getName()).isEqualTo(updateName);
                softly.assertThat(captured.getDescription()).isEqualTo(description);
            });
        }

        @DisplayName("PUT /categories/{code} - 존재하지 않는 카테고리 수정 시 예외 발생")
        @Test
        void updateCategory_Fail_NotFound() {
            // given
            String code = "UNKNOWN";
            var requestDto = CategoryRequest.CategoryUpdateDto.of("Name", "Desc");

            given(categoryService.update(any(CategoryCommand.Update.class)))
                    .willThrow(new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));

            // when & then
            RestApiException exception = assertThrows(RestApiException.class, () -> {
                categoryController.updateCategory(code, requestDto);
            });

            assertThat(exception.getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
    }
}