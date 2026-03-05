package com.jk.amazon2.integration;

import com.jk.amazon2.controller.dto.MemberRequest;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.MemberErrorCode;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MemberIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Faker faker = new Faker(Locale.of("ko"));

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @Nested
    @DisplayName("Member 생성 통합 테스트")
    class CreateMember {
        @DisplayName("[통합] POST /members - 생성 성공 및 DB 정합성 검증 [201 Created]")
        @Test
        void createMember_Integration_Success() {
            // given
            // 1. 카테고리 미리 생성
            String categoryCode = "DEV_TEST";
            String categoryName = "개발";
            String insertCategorySql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'system')";
            try {
                jdbcTemplate.update(insertCategorySql, categoryCode, categoryName, "카테고리");
            } catch (Exception e) {
                // 이미 존재하면 무시
            }

            String nickname = faker.name().fullName();
            // 닉네임 길이 제한(50자)에 맞게 자르기
            if (nickname.length() > 50) nickname = nickname.substring(0, 50);

            var requestDto = new MemberRequest.MemberCreateDto(nickname, categoryCode);

            // when & then (API 검증)
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(requestDto)
                    .when()
                    .post("/members")
                    .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("nickname", equalTo(nickname))
                    .body("categoryCode", equalTo(categoryCode));

            // then (DB 검증)
            String selectSql = "SELECT count(*) FROM member WHERE nickname = ? AND category_code = ?";
            Integer count = jdbcTemplate.queryForObject(selectSql, Integer.class, nickname, categoryCode);
            assertThat(count).isEqualTo(1);
        }

        @DisplayName("[통합] POST /members - 중복 닉네임으로 생성 실패 [409 Conflict]")
        @Test
        void createMember_Integration_Fail_DuplicateNickname() {
            // given
            String categoryCode = "DEV_DUP";
            String insertCategorySql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'system')";
            try {
                jdbcTemplate.update(insertCategorySql, categoryCode, "중복테스트용", "설명");
            } catch (Exception e) {
                // 이미 존재하면 무시
            }

            String nickname = "duplicate_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            var requestDto = new MemberRequest.MemberCreateDto(nickname, categoryCode);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(requestDto)
                    .when()
                    .post("/members")
                    .then()
                    .statusCode(HttpStatus.CONFLICT.value())
                    .body("code", equalTo(MemberErrorCode.MEMBER_NICKNAME_ALREADY_EXISTS.name()))
                    .body("message", equalTo(MemberErrorCode.MEMBER_NICKNAME_ALREADY_EXISTS.getMessage()));
        }

        @DisplayName("[통합] POST /members - 존재하지 않는 카테고리로 생성 실패 [404 Not Found]")
        @Test
        void createMember_Integration_Fail_CategoryNotFound() {
            // given
            String nickname = faker.name().fullName();
            if (nickname.length() > 50) nickname = nickname.substring(0, 50);
            String unknownCategoryCode = "UNKNOWN_CODE";

            var requestDto = new MemberRequest.MemberCreateDto(nickname, unknownCategoryCode);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(requestDto)
                    .when()
                    .post("/members")
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", equalTo(CategoryErrorCode.CATEGORY_NOT_FOUND.name()))
                    .body("message", equalTo(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage()));
        }

        @DisplayName("[통합] POST /members - 유효성 검사 실패 (닉네임) [400 Bad Request]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideInvalidMemberScenarios")
        void createMember_Integration_Fail_Validation(
                String scenario,
                String nickname,
                String categoryCode,
                String expectedMessage
        ) {
            // given
            var requestDto = new MemberRequest.MemberCreateDto(nickname, categoryCode);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(requestDto)
                    .when()
                    .post("/members")
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("message", equalTo(expectedMessage));
        }

        static Stream<Arguments> provideInvalidMemberScenarios() {
            return Stream.of(
                    Arguments.of("닉네임 공백", "", "DEV", MemberErrorCode.MEMBER_NICKNAME_INVALID.getMessage()),
                    Arguments.of("닉네임 null", null, "DEV", MemberErrorCode.MEMBER_NICKNAME_INVALID.getMessage()),
                    Arguments.of("닉네임 50자 초과", "a".repeat(51), "DEV", "닉네임은 최대 50자까지 입력 가능합니다.")
            );
        }
    }
}
