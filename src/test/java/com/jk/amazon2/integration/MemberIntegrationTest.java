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
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class MemberIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager em;

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

    @Nested
    @DisplayName("Member 목록 조회 통합 테스트")
    class GetMembers {

        @BeforeEach
        void setUpData() {
            String categoryInsertSql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'test')";
            jdbcTemplate.batchUpdate(categoryInsertSql, List.of(
                    new Object[]{"DEV_E2E", "개발", "개발팀"},
                    new Object[]{"DESIGN_E2E", "디자인", "디자인팀"}
            ));

            String memberInsertSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, NOW(), 'test', NOW(), 'test')";
            jdbcTemplate.batchUpdate(memberInsertSql, List.of(
                    new Object[]{"dev_user1", "DEV_E2E", false},
                    new Object[]{"dev_user2", "DEV_E2E", false},
                    new Object[]{"design_user", "DESIGN_E2E", false},
                    new Object[]{"deleted_user", "DEV_E2E", true}
            ));
        }

        @DisplayName("조회 성공 - 필터 조건별 결과 수 검증 [success]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideFilterConditions")
        void getMembers_success(String scenario, String nickname, String categoryCode, String status, int expectedTotal) {
            var spec = RestAssuredMockMvc.given();
            if (nickname != null) spec.param("nickname", nickname);
            if (categoryCode != null) spec.param("categoryCode", categoryCode);
            if (status != null) spec.param("status", status);

            spec.when().get("/members")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("totalElements", equalTo(expectedTotal));
        }

        static Stream<Arguments> provideFilterConditions() {
            return Stream.of(
                    Arguments.of("전체 조회", null, null, null, 4),
                    Arguments.of("nickname 필터", "dev", null, null, 2),
                    Arguments.of("categoryCode 필터", null, "DEV_E2E", null, 3),
                    Arguments.of("active 상태 필터", null, null, "active", 3),
                    Arguments.of("deleted 상태 필터", null, null, "deleted", 1)
            );
        }

        @Test
        @DisplayName("조회 성공 - categoryName JOIN 반환 확인 [success]")
        void getMembers_success_categoryName() {
            RestAssuredMockMvc.given()
                    .param("categoryCode", "DEV_E2E")
                    .when().get("/members")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("content[0].categoryName", equalTo("개발"));
        }

        @Test
        @DisplayName("조회 성공 - status 문자열 변환 확인 [success]")
        void getMembers_success_status_conversion() {
            RestAssuredMockMvc.given()
                    .param("nickname", "deleted_user")
                    .when().get("/members")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("totalElements", equalTo(1))
                    .body("content[0].status", equalTo("deleted"));
        }

        @Test
        @DisplayName("조회 성공 - 페이지네이션 구조 검증 [success]")
        void getMembers_success_pagination() {
            RestAssuredMockMvc.given()
                    .param("page", "0")
                    .param("size", "2")
                    .when().get("/members")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("totalElements", equalTo(4))
                    .body("content", hasSize(2))
                    .body("size", equalTo(2))
                    .body("number", equalTo(0));
        }

        @Test
        @DisplayName("조회 성공 - 빈 결과 [success]")
        void getMembers_success_empty() {
            RestAssuredMockMvc.given()
                    .param("nickname", "nonexistent_xyz")
                    .when().get("/members")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("totalElements", equalTo(0))
                    .body("content", hasSize(0));
        }
    }

    @Nested
    @DisplayName("Member 수정 통합 테스트")
    class UpdateMember {
        private Long memberId;

        @BeforeEach
        void setUpData() {
            String categorySql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'test')";
            jdbcTemplate.batchUpdate(categorySql, List.of(
                    new Object[]{"ORIGIN_CAT", "원래카테고리", "설명"},
                    new Object[]{"TARGET_CAT", "변경카테고리", "설명"}
            ));

            String memberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'test', NOW(), 'test')";
            jdbcTemplate.update(memberSql, "update_target_user", "ORIGIN_CAT");
            memberId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE nickname = ?", Long.class, "update_target_user");
        }

        @Test
        @DisplayName("[통합] PUT /members/{id} - 수정 성공 및 DB 정합성 검증 [200 OK]")
        void updateMember_success() {
            // given
            String newNickname = "updated_nickname";
            String newCategoryCode = "TARGET_CAT";
            var request = new MemberRequest.MemberDto(newNickname, newCategoryCode);

            // when && then
            RestAssuredMockMvc.given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .put("/members/{id}", memberId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("nickname", equalTo(newNickname))
                    .body("categoryCode", equalTo(newCategoryCode));
            em.flush();

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM member WHERE nickname = ? AND category_code = ?",
                    Integer.class, newNickname, newCategoryCode
            );
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("[통합] PUT /members/{id} - 존재하지 않는 회원 [404 Not Found]")
        void updateMember_fail_member_not_found() {
            // given
            Long nonExistentId = 999999L;
            var request = new MemberRequest.MemberDto("new_nickname", "ORIGIN_CAT");

            // when & then
            RestAssuredMockMvc.given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .put("/members/{id}", nonExistentId)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", equalTo(MemberErrorCode.MEMBER_NOT_FOUND.name()))
                    .body("message", equalTo(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("[통합] PUT /members/{id} - 존재하지 않는 카테고리 코드 [404 Not Found]")
        void updateMember_fail_category_not_found() {
            // given
            var request = new MemberRequest.MemberDto("new_nickname", "NO_CAT");

            // when & then
            RestAssuredMockMvc.given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .put("/members/{id}", memberId)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", equalTo(CategoryErrorCode.CATEGORY_NOT_FOUND.name()))
                    .body("message", equalTo(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage()));
        }

        @DisplayName("[통합] PUT /members/{id} - 유효성 검사 실패 [400 Bad Request]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideInvalidUpdateScenarios")
        void updateMember_fail_validation(String scenario, String nickname, String categoryCode, String expectedMessage) {
            // given
            var request = new MemberRequest.MemberDto(nickname, categoryCode);

            // when & then
            RestAssuredMockMvc.given()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .put("/members/{id}", memberId)
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("message", equalTo(expectedMessage));
        }

        static Stream<Arguments> provideInvalidUpdateScenarios() {
            return Stream.of(
                    Arguments.of("닉네임 공백", "", "TARGET_CAT", MemberErrorCode.MEMBER_NICKNAME_INVALID.getMessage()),
                    Arguments.of("닉네임 null", null, "TARGET_CAT", MemberErrorCode.MEMBER_NICKNAME_INVALID.getMessage()),
                    Arguments.of("닉네임 50자 초과", "a".repeat(51), "TARGET_CAT", MemberErrorCode.MEMBER_NICKNAME_INVALID.getMessage()),
                    Arguments.of("카테고리 코드 공백", "new_nickname", "", MemberErrorCode.MEMBER_CATEGORY_CODE_INVALID.getMessage()),
                    Arguments.of("카테고리 코드 10자 초과", "new_nickname", "a".repeat(11), MemberErrorCode.MEMBER_CATEGORY_CODE_INVALID.getMessage())
            );
        }
    }

    @Nested
    @DisplayName("Member 단건 조회 통합 테스트")
    class GetMember {
        private Long memberId;

        @BeforeEach
        void setUpData() {
            String categorySql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'test')";
            jdbcTemplate.update(categorySql, "GET_CAT", "조회테스트", "설명");

            String memberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'test', NOW(), 'test')";
            jdbcTemplate.update(memberSql, "get_member", "GET_CAT");
            memberId = jdbcTemplate.queryForObject("SELECT id FROM member WHERE nickname = ?", Long.class, "get_member");
        }

        @Test
        @DisplayName("[통합] GET /members/{id} - 조회 성공 [200 OK]")
        void getMember_success() {
            // when & then
            RestAssuredMockMvc.given()
                    .when()
                    .get("/members/{id}", memberId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("id", equalTo(memberId.intValue()))
                    .body("nickname", equalTo("get_member"))
                    .body("categoryCode", equalTo("GET_CAT"))
                    .body("status", equalTo("active"));

            // DB 검증
            em.flush();
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM member WHERE id = ? AND nickname = ? AND category_code = ?",
                    Integer.class, memberId, "get_member", "GET_CAT"
            );
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("[통합] GET /members/{id} - 존재하지 않는 회원 [404 Not Found]")
        void getMember_fail_not_found() {
            // given
            Long nonExistentId = 999999L;

            // when & then
            RestAssuredMockMvc.given()
                    .when()
                    .get("/members/{id}", nonExistentId)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", equalTo(MemberErrorCode.MEMBER_NOT_FOUND.name()))
                    .body("message", equalTo(MemberErrorCode.MEMBER_NOT_FOUND.getMessage()));
        }
    }
}
