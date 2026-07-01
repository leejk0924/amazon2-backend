package com.jk.amazon2.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.amazon2.member.dto.MemberRequest;
import com.jk.amazon2.member.exception.MemberErrorCode;
import com.jk.amazon2.common.exception.RestApiException;
import com.jk.amazon2.member.service.MemberService;
import com.jk.amazon2.member.dto.MemberCommand;
import com.jk.amazon2.member.dto.MemberResult;
import com.jk.amazon2.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class MemberControllerIntegrationTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @Nested
    @DisplayName("Member 수정 - 통합 테스트")
    class UpdateMember{
        @Test
        @DisplayName("회원 수정 성공 [success]")
        void updateMember_success() throws Exception {
            // given
            String nickname = "test_member";
            String categoryCode = "DESIGN";

            MemberRequest.MemberDto request = new MemberRequest.MemberDto("test-name", categoryCode);
            MemberResult.Update updateResult = MemberResult.Update.of(nickname, "test-name", categoryCode);

            given(memberService.update(any(MemberCommand.Update.class)))
                    .willReturn(updateResult);

            // when & then
            mockMvc.perform(put("/members/{nickname}", nickname)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value(nickname))
                    .andExpect(jsonPath("$.categoryCode").value(categoryCode));

            verify(memberService).update(any(MemberCommand.Update.class));
        }

        @Test
        @DisplayName("회원 수정 성공 - categoryCode null [success]")
        void updateMember_success_with_null_categoryCode() throws Exception {
            // given
            String nickname = "test_member";

            MemberRequest.MemberDto request = new MemberRequest.MemberDto("test-name", null);
            MemberResult.Update updateResult = MemberResult.Update.of(nickname, "test-name", null);

            given(memberService.update(any(MemberCommand.Update.class)))
                    .willReturn(updateResult);

            // when & then
            mockMvc.perform(put("/members/{nickname}", nickname)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value(nickname));

            verify(memberService).update(any(MemberCommand.Update.class));
        }

        @DisplayName("회원 수정 실패 케이스 [fail]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideUpdateMemberFailureCases")
        void updateMember_fail(String scenario, String name, String categoryCode, MemberErrorCode errorCode) throws Exception {
            // given
            MemberRequest.MemberDto request = new MemberRequest.MemberDto(name, categoryCode);

            given(memberService.update(any(MemberCommand.Update.class)))
                    .willThrow(new RestApiException(errorCode));

            // when & then
            mockMvc.perform(put("/members/{nickname}", "test_member")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());
        }

        private static Stream<Arguments> provideUpdateMemberFailureCases() {
            return Stream.of(
                    Arguments.of("존재하지 않는 회원", "test-name", "DESIGN", MemberErrorCode.MEMBER_NOT_FOUND),
                    Arguments.of("이름 null", null, "DESIGN", MemberErrorCode.MEMBER_NAME_INVALID),
                    Arguments.of("이름 공백", "", "DESIGN", MemberErrorCode.MEMBER_NAME_INVALID),
                    Arguments.of("이름 50자 초과", "a".repeat(51), "DESIGN", MemberErrorCode.MEMBER_NAME_INVALID),
                    Arguments.of("카테고리 코드 공백", "test-name", "", MemberErrorCode.MEMBER_CATEGORY_CODE_INVALID),
                    Arguments.of("카테고리 코드 10자 초과", "test-name", "a".repeat(11), MemberErrorCode.MEMBER_CATEGORY_CODE_INVALID)
            );
        }
    }

    @Nested
    @DisplayName("Member 조회 - 통합 테스트")
    class GetMembers {
        @DisplayName("회원 목록 조회 성공 [success]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideFilterConditions")
        void getMembers_success(
                String scenario,
                String nickname,
                String categoryCode,
                String status,
                int expectedCount
        ) throws Exception {
            // given
            Boolean isDeleted = status == null ? null : "deleted".equals(status);
            List<MemberResult.Summary> content = createFilteredContent(nickname, categoryCode, isDeleted, expectedCount);
            Page<MemberResult.Summary> pageResult = new PageImpl<>(content, PageRequest.of(0, 10), expectedCount);

            given(memberService.findMembers(any(MemberRequest.MemberSearchCondition.class), any(Pageable.class)))
                    .willReturn(pageResult);

            // when & then
            var request = get("/members");
            if (nickname != null) request.param("nickname", nickname);
            if (categoryCode != null) request.param("categoryCode", categoryCode);
            if (status != null) request.param("status", status);

            mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(expectedCount))
                    .andExpect(jsonPath("$.totalElements").value(expectedCount));

            verify(memberService).findMembers(any(MemberRequest.MemberSearchCondition.class), any(Pageable.class));
        }

        private static Stream<Arguments> provideFilterConditions() {
            return Stream.of(
                    of("조건 없음",                null,  null,  null,      5),
                    of("nickname 단일 필터",        "dev", null,  null,      3),
                    of("categoryCode 단일 필터",    null,  "DEV", null,      3),
                    of("active 상태 단일 필터",      null,  null,  "active",  3),
                    of("deleted 상태 단일 필터",     null,  null,  "deleted", 2),
                    of("nickname + categoryCode",  "dev", "DEV", null,      3),
                    of("categoryCode + active",    null,  "DEV", "active",  2),
                    of("nickname + active",        "dev", null,  "active",  2)
            );
        }

        private static List<MemberResult.Summary> createFilteredContent(
                String nickname,
                String categoryCode,
                Boolean isDeleted,
                int expectedCount
        ) {
            List<MemberResult.Summary> allData = List.of(
                    new MemberResult.Summary("dev_user1", "test-name", "DEV", LocalDateTime.now(), false),
                    new MemberResult.Summary("dev_user2", "name-2", "DEV", LocalDateTime.now(), false),
                    new MemberResult.Summary("design_user", "name-3", "DESIGN", LocalDateTime.now(), false),
                    new MemberResult.Summary("deleted_dev_user", "name-4", "DEV", LocalDateTime.now(), true),
                    new MemberResult.Summary("deleted_design_user", "name-5", "DESIGN", LocalDateTime.now(), true)
            );

            return allData.stream()
                    .filter(s -> nickname == null || s.nickname().contains(nickname))
                    .filter(s -> categoryCode == null || "DEV".equals(categoryCode) && "DEV".equals(s.categoryName()))
                    .filter(s -> isDeleted == null || s.deleted() == isDeleted)
                    .limit(expectedCount)
                    .toList();
        }

        @DisplayName("회원 목록 조회 성공 - 페이지네이션 [success]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("providePaginationScenarios")
        void getMembers_success_pagination(
                String scenario,
                int pageNumber,
                int pageSize,
                int totalElements
        ) throws Exception {
            // given
            List<MemberResult.Summary> content = new ArrayList<>();
            int start = pageNumber * pageSize;
            int end = Math.min(start + pageSize, totalElements);
            for (int i = start; i < end; i++) {
                content.add(new MemberResult.Summary("user" + i, "name-" + i, "DEV", LocalDateTime.now(), false));
            }

            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            Page<MemberResult.Summary> pageResult = new PageImpl<>(content, pageable, totalElements);

            given(memberService.findMembers(any(MemberRequest.MemberSearchCondition.class), any(Pageable.class)))
                    .willReturn(pageResult);

            // when & then
            mockMvc.perform(get("/members")
                            .param("page", String.valueOf(pageNumber))
                            .param("size", String.valueOf(pageSize)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(totalElements))
                    .andExpect(jsonPath("$.number").value(pageNumber))
                    .andExpect(jsonPath("$.size").value(pageSize));

            verify(memberService).findMembers(any(MemberRequest.MemberSearchCondition.class), any(Pageable.class));
        }

        private static Stream<Arguments> providePaginationScenarios() {
            return Stream.of(
                    of("페이지 크기 10 - 첫 페이지", 0, 10, 100),
                    of("페이지 크기 10 - 마지막 페이지", 9, 10, 100),
                    of("페이지 크기 25 - 첫 페이지", 0, 25, 100),
                    of("페이지 크기 25 - 마지막 페이지", 3, 25, 100),
                    of("페이지 크기 50 - 첫 페이지", 0, 50, 100),
                    of("페이지 크기 50 - 마지막 페이지", 1, 50, 100)
            );
        }

        @Test
        @DisplayName("회원 목록 조회 성공 - 빈 결과 [success]")
        void getMembers_success_empty_result() throws Exception {
            // given
            Page<MemberResult.Summary> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            given(memberService.findMembers(any(MemberRequest.MemberSearchCondition.class), any(Pageable.class)))
                    .willReturn(emptyPage);

            // when & then
            mockMvc.perform(get("/members")
                            .param("nickname", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("회원 목록 조회 성공 - status 변환 확인 [success]")
        void getMembers_success_status_conversion() throws Exception {
            // given
            List<MemberResult.Summary> content = List.of(
                    new MemberResult.Summary("dev_user1", "test-name", "DEV", LocalDateTime.now(), false),
                    new MemberResult.Summary("deleted_dev_user", "name-2", "DEV", LocalDateTime.now(), true)
            );
            Page<MemberResult.Summary> pageResult = new PageImpl<>(content, PageRequest.of(0, 10), 2);

            given(memberService.findMembers(any(MemberRequest.MemberSearchCondition.class), any(Pageable.class)))
                    .willReturn(pageResult);

            // when & then
            mockMvc.perform(get("/members"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].status").value("active"))
                    .andExpect(jsonPath("$.content[1].status").value("deleted"));
        }
    }

    @Nested
    @DisplayName("Member 영구 삭제 - 통합 테스트")
    class HardDeleteMember {
        @Test
        @DisplayName("회원 영구 삭제 성공 [success]")
        void hardDeleteMember_success() throws Exception {
            // given
            String nickname = "test_member";

            // when & then
            mockMvc.perform(delete("/members/{nickname}/permanent", nickname))
                    .andExpect(status().isNoContent());

            verify(memberService).hardDelete(nickname);
        }

        @Test
        @DisplayName("회원 영구 삭제 실패 - 소프트 삭제되지 않은 회원 [fail]")
        void hardDeleteMember_fail_not_deleted() throws Exception {
            // given
            String nickname = "test_member";
            org.mockito.Mockito.doThrow(new RestApiException(MemberErrorCode.MEMBER_NOT_DELETED))
                    .when(memberService).hardDelete(nickname);

            // when & then
            mockMvc.perform(delete("/members/{nickname}/permanent", nickname))
                    .andExpect(status().isBadRequest());
        }
    }
}
