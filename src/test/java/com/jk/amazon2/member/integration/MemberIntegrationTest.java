package com.jk.amazon2.member.integration;

import com.jk.amazon2.member.dto.MemberRequest;
import com.jk.amazon2.category.exception.CategoryErrorCode;
import com.jk.amazon2.member.exception.MemberErrorCode;
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
            try {
                jdbcTemplate.update(insertMemberSql, nickname, categoryCode);
            } catch (Exception e) {
                // 이미 존재하면 무시
            }

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
                    .body("code", equalTo(MemberErrorCode.MEMBER_NICKNAME_ALREADY_EXISTS.name()));
        }

        @DisplayName("[통합] POST /members - 존재하지 않는 카테고리로 생성 실패 [400 Bad Request]")
        @Test
        @org.junit.jupiter.api.Disabled("카테고리 검증 부분 수정 필요 - 별도 PR에서 해결")
        void createMember_Integration_Fail_CategoryNotFound() {
            // given
            // 카테고리 10자 이상 코드는 검증 에러가 발생하므로, 10자 이하로 설정
            String categoryCode = "UNKNOWN";
            String insertCategorySql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'system')";
            try {
                jdbcTemplate.update(insertCategorySql, "VALID_CAT", "유효한카테고리", "설명");
            } catch (Exception e) {
                // 이미 존재하면 무시
            }

            String nickname = "new_user";
            // 존재하지 않는 카테고리 코드 (10자 이하)
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
                    .body("code", equalTo(CategoryErrorCode.CATEGORY_NOT_FOUND.name()));
        }
    }

    @Nested
    @DisplayName("Member 조회 통합 테스트")
    class GetMembers {
        private String categoryCode = "TEAM";

        @BeforeEach
        void setUp() {
            String insertCategorySql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'system')";
            try {
                jdbcTemplate.update(insertCategorySql, categoryCode, "팀", "팀 카테고리");
            } catch (Exception e) {
                // 이미 존재하면 무시
            }
        }

        @DisplayName("[통합] GET /members/{id} - 회원 단건 조회 성공 [200 OK]")
        @Test
        void getMember_Integration_Success() {
            // given
            String nickname = "test_user_001";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            String selectIdSql = "SELECT id FROM member WHERE nickname = ?";
            Long memberId = jdbcTemplate.queryForObject(selectIdSql, Long.class, nickname);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .get("/members/{id}", memberId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("nickname", equalTo(nickname))
                    .body("status", equalTo("active"));
        }

        @DisplayName("[통합] GET /members - 회원 목록 조회 성공 [200 OK]")
        @Test
        void getMembers_Integration_Success() {
            // given
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            for (int i = 0; i < 5; i++) {
                jdbcTemplate.update(insertMemberSql, "user_" + i, categoryCode);
            }

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .get("/members")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("content", hasSize(5))
                    .body("totalElements", equalTo(5));
        }

        @DisplayName("[통합] GET /members - nickname 필터 적용 [200 OK]")
        @Test
        void getMembers_Integration_Filter_Nickname() {
            // given
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, "filter_user", categoryCode);
            jdbcTemplate.update(insertMemberSql, "other_user", categoryCode);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .queryParam("nickname", "filter")
                    .when()
                    .get("/members")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("content", hasSize(1))
                    .body("totalElements", equalTo(1));
        }

        @DisplayName("[통합] GET /members - status 필터 적용 [200 OK]")
        @Test
        void getMembers_Integration_Filter_Status() {
            // given
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, "active_user", categoryCode, false);
            jdbcTemplate.update(insertMemberSql, "deleted_user", categoryCode, true);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .queryParam("status", "active")
                    .when()
                    .get("/members")
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("content", hasSize(1))
                    .body("totalElements", equalTo(1))
                    .body("content[0].status", equalTo("active"));
        }
    }

    @Nested
    @DisplayName("Member 수정 통합 테스트")
    class UpdateMember {
        private String categoryCode = "DEV";

        @BeforeEach
        void setUp() {
            String insertCategorySql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'system')";
            try {
                jdbcTemplate.update(insertCategorySql, categoryCode, "개발", "개발팀");
                jdbcTemplate.update(insertCategorySql, "DESIGN", "디자인", "디자인팀");
            } catch (Exception e) {
                // 이미 존재하면 무시
            }
        }

        @DisplayName("[통합] PUT /members/{id} - 회원 수정 성공 [200 OK]")
        @Test
        void updateMember_Integration_Success() {
            // given
            String nickname = "original_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            String selectIdSql = "SELECT id FROM member WHERE nickname = ?";
            Long memberId = jdbcTemplate.queryForObject(selectIdSql, Long.class, nickname);

            String updatedNickname = "updated_user";
            String updatedCategoryCode = "DESIGN";
            var requestDto = new MemberRequest.MemberDto(updatedNickname, updatedCategoryCode);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(requestDto)
                    .when()
                    .put("/members/{id}", memberId)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("nickname", equalTo(updatedNickname))
                    .body("categoryCode", equalTo(updatedCategoryCode));
        }
    }

    @Nested
    @DisplayName("Member 삭제 통합 테스트")
    class DeleteMember {
        private String categoryCode = "DEV";

        @BeforeEach
        void setUp() {
            String insertCategorySql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'system')";
            try {
                jdbcTemplate.update(insertCategorySql, categoryCode, "개발", "개발팀");
            } catch (Exception e) {
                // 이미 존재하면 무시
            }
        }

        @DisplayName("[통합] DELETE /members/{id} - 회원 소프트 삭제 성공 [204 No Content]")
        @Test
        void deleteMember_Integration_Success() {
            // given
            String nickname = "delete_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            String selectIdSql = "SELECT id FROM member WHERE nickname = ?";
            Long memberId = jdbcTemplate.queryForObject(selectIdSql, Long.class, nickname);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .delete("/members/{id}", memberId)
                    .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());

            // DB 검증 - deleted = true (flush 후 확인)
            em.flush();
            String selectDeletedSql = "SELECT deleted FROM member WHERE id = ?";
            Boolean deleted = jdbcTemplate.queryForObject(selectDeletedSql, Boolean.class, memberId);
            assertThat(deleted).isTrue();
        }

        @DisplayName("[통합] DELETE /members/{id}/permanent - 회원 영구 삭제 성공 [204 No Content]")
        @Test
        void hardDeleteMember_Integration_Success() {
            // given
            String nickname = "hard_delete_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode, true);

            String selectIdSql = "SELECT id FROM member WHERE nickname = ?";
            Long memberId = jdbcTemplate.queryForObject(selectIdSql, Long.class, nickname);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .delete("/members/{id}/permanent", memberId)
                    .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());

            // DB 검증 - 완전 삭제 (flush 후 확인)
            em.flush();
            String selectSql = "SELECT count(*) FROM member WHERE id = ?";
            Integer count = jdbcTemplate.queryForObject(selectSql, Integer.class, memberId);
            assertThat(count).isEqualTo(0);
        }

        @DisplayName("[통합] DELETE /members/{id}/permanent - 소프트 삭제되지 않은 회원 영구 삭제 실패 [400 Bad Request]")
        @Test
        void hardDeleteMember_Integration_Fail_NotDeleted() {
            // given
            String nickname = "not_deleted_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            String selectIdSql = "SELECT id FROM member WHERE nickname = ?";
            Long memberId = jdbcTemplate.queryForObject(selectIdSql, Long.class, nickname);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .delete("/members/{id}/permanent", memberId)
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo(MemberErrorCode.MEMBER_NOT_DELETED.name()));
        }
    }
}
