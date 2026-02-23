package com.jk.amazon2.integration;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.testsupport.IntegrationTestSupport;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

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

        var requestDto = new CategoryRequest.CategoryCreateDto(code, name, description);

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

        // then (DB에 저장되었는지 검증)
        var sql = "SELECT name FROM blog_category WHERE code = ?";
        String savedName = jdbcTemplate.queryForObject(sql, String.class, code);
        assertThat(savedName).isEqualTo(name);
    }

    @DisplayName("[통합] POST /categories - 중복 코드로 인한 생성 실패 [409 Conflict]")
    @Test
    void createCategory_Integration_fail_DuplicateCode() throws Exception {
        // given
        String code = faker.regexify("[A-Z]{5,10}");
        String name = faker.company().industry();
        String description = faker.lorem().sentence();

        String initSql = """
                    INSERT INTO blog_category (code, name, description) VALUES(?, ?, ?)
                """;
        jdbcTemplate.update(initSql, code, name,description);

        var requestSameCodeDto = new CategoryRequest.CategoryCreateDto(code, name, description);

        // when & then
        RestAssuredMockMvc
                .given()
                .contentType(ContentType.JSON)
                .body(requestSameCodeDto)
                .when()
                .post("/categories")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo(CategoryErrorCode.CATEGORY_ALREADY_EXISTS.name()))
                .body("message", equalTo(CategoryErrorCode.CATEGORY_ALREADY_EXISTS.getMessage()));

        // then - DB에 하나만 존재하는지 확인
        var testSql = "SELECT count(*) FROM blog_category";
        Integer count = jdbcTemplate.queryForObject(testSql, Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
