package com.jk.amazon2.controller;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.controller.dto.CategoryResponse;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.RestApiException;
import com.jk.amazon2.service.CategoryService;
import com.jk.amazon2.service.dto.CategoryCommand;
import com.jk.amazon2.service.dto.CategoryResult;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import net.datafaker.Faker;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@WebMvcTest
class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private AuditorAware<String> auditorProvider;
    @Autowired
    private CategoryController categoryController;

    @DisplayName("Category 생성 - API 테스트")
    @Nested

    class CreateCategory {
        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.mockMvc(mockMvc);
        }

        @DisplayName("POST /categories - 카테고리 생성 성공 [201 Created]")
        @Test
        void createCategory_return201AndBody_whenRequestIsValid() {
            // given
            Faker faker = new Faker(Locale.of("ko"));
            String code = faker.regexify("[A-Z]{5,10}");
            String name = faker.company().industry();
            String description = faker.lorem().sentence();
            var requestDto = new CategoryRequest.CategoryCreateDto(
                    code, name, description
            );

            CategoryResult.Detail mockResult = CategoryResult.Detail.of(
                    code, name, description,
                    LocalDateTime.now(), "admin"
            );

            given(categoryService.create(any(CategoryCommand.Create.class)))
                    .willReturn(mockResult);

            // when & then (API 검증)
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(requestDto)
                    .when()
                    .post("/categories")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("categoryCode", equalTo(code))
                    .body("categoryName", equalTo(name))
                    .body("description", equalTo(description));

            // then (서비스로 넘어간 데이터의 정확성 검증)
            ArgumentCaptor<CategoryCommand.Create> commandCaptor = ArgumentCaptor.forClass(CategoryCommand.Create.class);
            verify(categoryService).create(commandCaptor.capture());
            CategoryCommand.Create capturedCommand = commandCaptor.getValue();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedCommand.getCode())
                        .as("DTO의 code가 Command의 code 검증")
                        .isEqualTo(code);
                softly.assertThat(capturedCommand.getName())
                        .as("DTO의 name이 Command로 정확히 매핑되었는가")
                        .isEqualTo(name);
                softly.assertThat(capturedCommand.getDescription())
                        .as("DTO의 description이 Command로 정확히 매핑되었는가")
                        .isEqualTo(description);
            });
        }

        @DisplayName("POST /categories - 유효하지 않은 입력값에 대한 예외 처리 검증")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideInvalidCategoryRequests")
        void createCategory_ShouldFail_WhenRequestIsInvalid(
                String testCase,
                String code,
                String name,
                String description,
                int statusCode,
                String errorCode,
                String errorMessage
        ) {
            // given
            var requestDto = new CategoryRequest.CategoryCreateDto(code, name, description);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(requestDto)
                    .when()
                    .post("/categories")
                    .then()
                    .statusCode(statusCode)
                    .body("code", equalTo(errorCode))
                    .body("message", equalTo(errorMessage));
        }

        static Stream<Arguments> provideInvalidCategoryRequests() {
            Faker testFaker = new Faker(Locale.of("ko"));
            return Stream.of(
                    // code Test Case
                    Arguments.of(
                            "코드가 blank인 경우",
                            "",
                            testFaker.company().industry(),
                            testFaker.lorem().sentence(),
                            HttpStatus.BAD_REQUEST.value(),
                            "INVALID_INPUT",
                            "카테고리 코드는 필수 입니다."
                    ),
                    Arguments.of(
                            "코드가 null인 경우",
                            null,
                            testFaker.company().industry(),
                            testFaker.lorem().sentence(),
                            HttpStatus.BAD_REQUEST.value(),
                            "INVALID_INPUT",
                            "카테고리 코드는 필수 입니다."
                    ),
                    Arguments.of(
                            "코드가 10자 초과인 경우",
                            testFaker.regexify("[A-Z]{15}"),
                            testFaker.company().industry(),
                            testFaker.lorem().sentence(),
                            HttpStatus.BAD_REQUEST.value(),
                            "INVALID_INPUT",
                            "카테고리 코드는 최대 10자까지 입력 가능합니다."
                    ),

                    // name Test Case
                    Arguments.of(
                            "이름이 null인 경우",
                            testFaker.regexify("[A-Z]{5,10}"),
                            null,
                            testFaker.lorem().sentence(),
                            HttpStatus.BAD_REQUEST.value(),
                            "INVALID_INPUT",
                            "카테고리 이름은 필수 입니다."
                    ),
                    Arguments.of(
                            "이름이 blank인 경우",
                            testFaker.regexify("[A-Z]{5,10}"),
                            "",
                            testFaker.lorem().sentence(),
                            HttpStatus.BAD_REQUEST.value(),
                            "INVALID_INPUT",
                            "카테고리 이름은 필수 입니다."
                    ),
                    Arguments.of(
                            "이름이 50자 초과인 경우",
                            testFaker.regexify("[A-Z]{5,10}"),
                            testFaker.lorem().characters(51),
                            testFaker.lorem().sentence(),
                            HttpStatus.BAD_REQUEST.value(),
                            "INVALID_INPUT",
                            "카테고리 이름은 최대 50자까지 입력 가능합니다."
                    )
            );
        }
    }

    @Nested
    @DisplayName("Category 수정 - API 테스트")
    class UpdateCategory {

        @BeforeEach
        void setUp() {
            RestAssuredMockMvc.mockMvc(mockMvc);
        }

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
            // 1. 상태 코드 검증
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

        @DisplayName("PUT /categories/{code} - 존재하지 않는 카테고리 수정 시 404 Not Found 반환")
        @Test
        void updateCategory_Fail_NotFound() {
            // given
            String code = "UNKNOWN";
            String updateName = "Update Name";
            String description = "Update Description";

            // Request DTO 생성
            var requestDto = CategoryRequest.CategoryUpdateDto.of(updateName, description);

            // Service가 예외를 던지도록 Mocking 설정
            given(categoryService.update(any(CategoryCommand.Update.class)))
                    .willThrow(new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND));

            // when & then
            // 예외가 발생하는지 검증 (assertThrows 사용)
            RestApiException exception = assertThrows(RestApiException.class, () -> {
                categoryController.updateCategory(code, requestDto);
            });

            // 예외 내용 검증
            assertThat(exception.getErrorCode()).isEqualTo(CategoryErrorCode.CATEGORY_NOT_FOUND);
        }
    }
    @DisplayName("카테고리 수정 실패 - Service 예외 발생")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideServiceExceptions")
    void updateCategory_Fail_ServiceException(
            String testCase,
            CategoryErrorCode errorCode
    ) {
        // given
        String code = "TECH";
        var requestDto = CategoryRequest.CategoryUpdateDto.of("Name", "Desc");

        given(categoryService.update(any()))
                .willThrow(new RestApiException(errorCode));

        // when & then
        RestApiException exception = assertThrows(RestApiException.class,
                () -> categoryController.updateCategory(code, requestDto)
        );

        assertThat(exception.getErrorCode()).isEqualTo(errorCode);
    }
    static Stream<Arguments> provideServiceExceptions() {
        return Stream.of(
                Arguments.of("존재하지 않는 카테고리", CategoryErrorCode.CATEGORY_NOT_FOUND),
                Arguments.of("중복되는 카테고리 이름", CategoryErrorCode.CATEGORY_ALREADY_EXISTS)
        );
    }
}