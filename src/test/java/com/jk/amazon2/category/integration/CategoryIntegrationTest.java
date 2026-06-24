package com.jk.amazon2.category.integration;

import com.jk.amazon2.category.exception.CategoryErrorCode;
import com.jk.amazon2.testsupport.CategoryMother;
import com.jk.amazon2.testsupport.IntegrationTestSupport;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CategoryIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Faker faker = new Faker(Locale.of("ko"));

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("[통합] POST /categories - 생성 성공 및 DB 정합성 검증 [201 Created]")
    @Test
    void createCategory_Integration_Success() throws Exception {
        // given
        String code = faker.regexify("[A-Z]{5,10}");
        String name = faker.company().industry();
        String description = faker.lorem().sentence();

        // when & then (API 검증)
        RestAssuredMockMvc
                .given()
                .contentType(ContentType.JSON)
                .body(CategoryMother.createDto(code, name, description))
                .when()
                .post("/categories")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("categoryCode", equalTo(code))
                .body("categoryName", equalTo(name))
                .body("description", equalTo(description));

        // then (DB에 저장되었는지 검증)
        String savedName = jdbcTemplate.queryForObject(
                "SELECT name FROM blog_category WHERE code = ?", String.class, code);
        assertThat(savedName).isEqualTo(name);
    }

    @DisplayName("[통합] POST /categories - 중복 코드로 인한 생성 실패 [409 Conflict]")
    @Test
    void createCategory_Integration_fail_DuplicateCode() throws Exception {
        // given
        String code = faker.regexify("[A-Z]{5,10}");
        String name = faker.company().industry();
        String description = faker.lorem().sentence();

        jdbcTemplate.update(CategoryMother.INSERT_SQL, CategoryMother.fullParams(code, name, description));

        // when & then
        RestAssuredMockMvc
                .given()
                .contentType(ContentType.JSON)
                .body(CategoryMother.createDto(code, name, description))
                .when()
                .post("/categories")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo(CategoryErrorCode.CATEGORY_ALREADY_EXISTS.name()))
                .body("message", equalTo(CategoryErrorCode.CATEGORY_ALREADY_EXISTS.getMessage()));

        // then - DB에 하나만 존재하는지 확인
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM blog_category", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Nested
    @DisplayName("Category 수정 통합 테스트")
    class UpdateCategory {

        @DisplayName("[통합] PUT /categories/{code} - 수정 성공 및 DB 반영 확인 [200 OK]")
        @Test
        void updateCategory_Integration_Success() {
            // given
            String code = "TECH";
            jdbcTemplate.update(CategoryMother.INSERT_SQL,
                    CategoryMother.fullParams(code, "Original Name", "Original Description"));

            String updateName = "Updated Name";
            String updateDesc = "Updated Description";

            // when & then (API 호출 및 응답 검증)
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(CategoryMother.updateDto(updateName, updateDesc))
                    .when()
                    .put("/categories/{code}", code)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("categoryCode", equalTo(code))
                    .body("categoryName", equalTo(updateName))
                    .body("description", equalTo(updateDesc));

            // then (DB 데이터 변경 확인)
            Map<String, Object> result = jdbcTemplate.queryForMap(
                    "SELECT name, description FROM blog_category WHERE code = ?", code);
            assertThat(result.get("name")).isEqualTo(updateName);
            assertThat(result.get("description")).isEqualTo(updateDesc);
        }

        @DisplayName("[통합] PUT /categories/{code} - 수정 실패 케이스 검증 [400 Bad Request]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideInvalidUpdateScenarios")
        void updateCategory_Integration_Fail(
                String testCase,
                String name,
                String description,
                String expectedErrorMessage
        ) {
            // given
            String code = "VALID_TEST";
            jdbcTemplate.update(CategoryMother.INSERT_SQL,
                    CategoryMother.fullParams(code, "Original", "Original Description"));

            // when & then
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(CategoryMother.updateDto(name, description))
                    .when()
                    .put("/categories/{code}", code)
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo("INVALID_INPUT"))
                    .body("message", equalTo(expectedErrorMessage));
        }

        static Stream<Arguments> provideInvalidUpdateScenarios() {
            return Stream.of(
                    Arguments.of("이름이 null인 경우", null, "Desc", "카테고리 이름은 필수 입니다."),
                    Arguments.of("이름이 빈 문자열인 경우", "", "Desc", "카테고리 이름은 필수 입니다."),
                    Arguments.of("이름이 너무 긴 경우", "A".repeat(51), "Desc", "카테고리 이름은 최대 50자까지 입력 가능합니다.")
            );
        }
    }

    @Nested
    @DisplayName("Category 조회 통합 테스트")
    class GetCategory {

        @DisplayName("[통합] GET /categories/{code} - 단건 조회 성공 [200 OK]")
        @Test
        void getCategory_Integration_Success() {
            // given
            String code = "READ_TEST";
            String name = "Read Name";
            String description = "Read Desc";
            jdbcTemplate.update(CategoryMother.INSERT_SQL, CategoryMother.fullParams(code, name, description));

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .get("/categories/{code}", code)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("categoryCode", equalTo(code))
                    .body("categoryName", equalTo(name))
                    .body("description", equalTo(description));
        }

        @DisplayName("[통합] GET /categories/{code} - 존재하지 않는 카테고리 조회 시 실패 [404 Not Found]")
        @Test
        void getCategory_Integration_Fail_NotFound() {
            // given
            String unknownCode = "UNKNOWN_ID";

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .get("/categories/{code}", unknownCode)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", equalTo(CategoryErrorCode.CATEGORY_NOT_FOUND.name()))
                    .body("message", equalTo(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage()));
        }

        @DisplayName("[통합] GET /categories - 목록 조회 및 검색 성공")
        @Test
        void getCategories_Integration_Success() {
            // given
            jdbcTemplate.update(CategoryMother.INSERT_SQL, CategoryMother.fullParams("SEARCH_1", "Search Target 1", "Desc 1"));
            jdbcTemplate.update(CategoryMother.INSERT_SQL, CategoryMother.fullParams("SEARCH_2", "Search Target 2", "Desc 2"));
            jdbcTemplate.update(CategoryMother.INSERT_SQL, CategoryMother.fullParams("OTHER_3", "Other Category", "Desc 3"));

            // when & then
            RestAssuredMockMvc
                    .given()
                    .param("page", 0)
                    .param("size", 10)
                    .param("name", "Search")
                    .when()
                    .get("/categories")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("content.size()", equalTo(2))
                    .body("content[0].categoryCode", equalTo("SEARCH_1"))
                    .body("content[1].categoryCode", equalTo("SEARCH_2"))
                    .body("totalElements", equalTo(2));
        }

        @DisplayName("[통합] GET /categories - 페이징 및 정렬 동작 확인 [200 OK]")
        @Test
        void getCategories_Integration_PagingAndSorting() {
            // given — 테스트 데이터 15개
            for (int i = 1; i <= 15; i++) {
                String suffix = String.format("%02d", i);
                jdbcTemplate.update(CategoryMother.INSERT_SQL,
                        CategoryMother.fullParams("CODE" + suffix, "Name" + suffix, "Desc" + suffix));
            }

            // when & then — 첫 번째 페이지 (size=10, name 역순)
            RestAssuredMockMvc
                    .given()
                    .param("page", 0)
                    .param("size", 10)
                    .param("sort", "name,desc")
                    .when()
                    .get("/categories")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("content.size()", equalTo(10))
                    .body("totalElements", equalTo(15))
                    .body("totalPages", equalTo(2))
                    .body("first", equalTo(true))
                    .body("last", equalTo(false))
                    .body("content[0].categoryName", equalTo("Name15"))
                    .body("content[9].categoryName", equalTo("Name06"));

            RestAssuredMockMvc
                    .given()
                    .param("page", 1)
                    .param("size", 10)
                    .param("sort", "name,desc")
                    .when()
                    .get("/categories")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("content.size()", equalTo(5))
                    .body("first", equalTo(false))
                    .body("last", equalTo(true))
                    .body("content[0].categoryName", equalTo("Name05"))
                    .body("content[4].categoryName", equalTo("Name01"));
        }

        @DisplayName("[통합] GET /categories - 검색 결과가 없을 경우 빈 목록 반환 [200 OK]")
        @Test
        void getCategories_Integration_EmptyResult() {
            // when & then
            RestAssuredMockMvc
                    .given()
                    .param("name", "NonExistentName")
                    .when()
                    .get("/categories")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("content.size()", equalTo(0))
                    .body("totalElements", equalTo(0));
        }
    }
}
