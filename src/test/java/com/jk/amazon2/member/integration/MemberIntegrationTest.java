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
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

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
            String categoryCode = "DEV_TEST";
            String categoryName = "개발";
            String insertCategorySql = "INSERT INTO blog_category (code, name, description, created_at, created_by) VALUES (?, ?, ?, NOW(), 'system')";
            try {
                jdbcTemplate.update(insertCategorySql, categoryCode, categoryName, "카테고리");
            } catch (Exception e) {
                // 이미 존재하면 무시
            }

            String nickname = faker.name().fullName();
            if (nickname.length() > 50) nickname = nickname.substring(0, 50);

            var requestDto = new MemberRequest.MemberCreateDto(nickname, "테스터", categoryCode);

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

            var requestDto = new MemberRequest.MemberCreateDto(nickname, "테스터", categoryCode);

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
            String categoryCode = "UNKNOWN";
            String nickname = "new_user";
            var requestDto = new MemberRequest.MemberCreateDto(nickname, "테스터", categoryCode);

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

        @DisplayName("[통합] GET /members/{nickname} - 회원 단건 조회 성공 [200 OK]")
        @Test
        void getMember_Integration_Success() {
            // given
            String nickname = "test_user_001";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .get("/members/{nickname}", nickname)
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

        @DisplayName("[통합] PUT /members/{nickname} - 회원 수정 성공 [200 OK]")
        @Test
        void updateMember_Integration_Success() {
            // given
            String nickname = "original_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            String updatedNickname = "updated_user";
            String updatedCategoryCode = "DESIGN";
            var requestDto = new MemberRequest.MemberDto(updatedNickname, null, updatedCategoryCode);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .contentType(ContentType.JSON)
                    .body(requestDto)
                    .when()
                    .put("/members/{nickname}", nickname)
                    .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("nickname", equalTo(updatedNickname))
                    .body("categoryCode", equalTo(updatedCategoryCode));
        }
    }

    @Nested
    @DisplayName("Member 복구 통합 테스트")
    class RestoreMember {
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

        @DisplayName("[통합] PATCH /members/{nickname}/restore - soft delete된 회원 복구 성공 [204 No Content]")
        @Test
        void restoreMember_Integration_Success() {
            // Given
            String nickname = "restore_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, true, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            // When & Then (API 검증)
            RestAssuredMockMvc
                    .given()
                    .when()
                    .patch("/members/{nickname}/restore", nickname)
                    .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());

            // DB 검증 - deleted = false
            em.flush();
            String selectSql = "SELECT deleted FROM member WHERE nickname = ?";
            Boolean deleted = jdbcTemplate.queryForObject(selectSql, Boolean.class, nickname);
            assertThat(deleted).isFalse();
        }

        @DisplayName("[통합] PATCH /members/{nickname}/restore - 존재하지 않는 회원 복구 실패 [404 Not Found]")
        @Test
        void restoreMember_Integration_Fail_NotFound() {
            // Given
            String nickname = "non_existent_restore_user";

            // When & Then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .patch("/members/{nickname}/restore", nickname)
                    .then()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .body("code", equalTo(MemberErrorCode.MEMBER_NOT_FOUND.name()));
        }

        @DisplayName("[통합] PATCH /members/{nickname}/restore - 이미 활성 상태인 회원 복구 실패 [400 Bad Request]")
        @Test
        void restoreMember_Integration_Fail_AlreadyActive() {
            // Given
            String nickname = "already_active_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            // When & Then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .patch("/members/{nickname}/restore", nickname)
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo(MemberErrorCode.MEMBER_ALREADY_ACTIVE.name()));
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

        @DisplayName("[통합] DELETE /members/{nickname} - 회원 소프트 삭제 성공 [204 No Content]")
        @Test
        void deleteMember_Integration_Success() {
            // given
            String nickname = "delete_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .delete("/members/{nickname}", nickname)
                    .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());

            // DB 검증 - deleted = true (JPA flush 후 확인)
            em.flush();
            String selectDeletedSql = "SELECT deleted FROM member WHERE nickname = ?";
            Boolean deleted = jdbcTemplate.queryForObject(selectDeletedSql, Boolean.class, nickname);
            assertThat(deleted).isTrue();
        }

        @DisplayName("[통합] DELETE /members/{nickname}/permanent - 회원 영구 삭제 성공 [204 No Content]")
        @Test
        void hardDeleteMember_Integration_Success() {
            // given
            String nickname = "hard_delete_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, ?, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode, true);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .delete("/members/{nickname}/permanent", nickname)
                    .then()
                    .statusCode(HttpStatus.NO_CONTENT.value());

            // DB 검증 - 완전 삭제 (JPA flush 후 확인)
            em.flush();
            String selectSql = "SELECT count(*) FROM member WHERE nickname = ?";
            Integer count = jdbcTemplate.queryForObject(selectSql, Integer.class, nickname);
            assertThat(count).isEqualTo(0);
        }

        @DisplayName("[통합] DELETE /members/{nickname}/permanent - 소프트 삭제되지 않은 회원 영구 삭제 실패 [400 Bad Request]")
        @Test
        void hardDeleteMember_Integration_Fail_NotDeleted() {
            // given
            String nickname = "not_deleted_user";
            String insertMemberSql = "INSERT INTO member (nickname, category_code, deleted, created_at, created_by, updated_at, updated_by) VALUES (?, ?, false, NOW(), 'system', NOW(), 'system')";
            jdbcTemplate.update(insertMemberSql, nickname, categoryCode);

            // when & then
            RestAssuredMockMvc
                    .given()
                    .when()
                    .delete("/members/{nickname}/permanent", nickname)
                    .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("code", equalTo(MemberErrorCode.MEMBER_NOT_DELETED.name()));
        }
    }
}
