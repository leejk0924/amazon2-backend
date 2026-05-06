package com.jk.amazon2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.amazon2.controller.dto.MemberRequest;
import com.jk.amazon2.exception.MemberErrorCode;
import com.jk.amazon2.exception.RestApiException;
import com.jk.amazon2.service.MemberService;
import com.jk.amazon2.service.dto.MemberCommand;
import com.jk.amazon2.service.dto.MemberResult;
import com.jk.amazon2.testsupport.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
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
            Long memberId = 1L;
            String nickname = "updated_member";
            String categoryCode = "DESIGN";

            MemberRequest.MemberDto request = new MemberRequest.MemberDto(nickname, categoryCode);
            MemberResult.Update updateResult = MemberResult.Update.of(nickname, categoryCode);

            given(memberService.update(any(MemberCommand.Update.class)))
                    .willReturn(updateResult);

            // when & then
            mockMvc.perform(put("/members/{id}", memberId)
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
            Long memberId = 1L;
            String nickname = "updated_member";

            MemberRequest.MemberDto request = new MemberRequest.MemberDto(nickname, null);
            MemberResult.Update updateResult = MemberResult.Update.of(nickname, null);

            given(memberService.update(any(MemberCommand.Update.class)))
                    .willReturn(updateResult);

            // when & then
            mockMvc.perform(put("/members/{id}", memberId)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nickname").value(nickname));

            verify(memberService).update(any(MemberCommand.Update.class));
        }

        @DisplayName("회원 수정 실패 케이스 [fail]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideUpdateMemberFailureCases")
        void updateMember_fail(String scenario, String nickname, String categoryCode, MemberErrorCode errorCode) throws Exception {
            // given
            Long memberId = 1L;
            MemberRequest.MemberDto request = new MemberRequest.MemberDto(nickname, categoryCode);

            given(memberService.update(any(MemberCommand.Update.class)))
                    .willThrow(new RestApiException(errorCode));

            // when & then
            mockMvc.perform(put("/members/{id}", memberId)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());
        }

        private static Stream<Arguments> provideUpdateMemberFailureCases() {
            return Stream.of(
                    Arguments.of("존재하지 않는 회원", "test_member", "DESIGN", MemberErrorCode.MEMBER_NICKNAME_NOT_FOUND),
                    Arguments.of("닉네임 공백", "", "DESIGN", MemberErrorCode.MEMBER_NICKNAME_INVALID),
                    Arguments.of("닉네임 null", null, "DESIGN", MemberErrorCode.MEMBER_NICKNAME_INVALID),
                    Arguments.of("닉네임 50자 초과", "a".repeat(51), "DESIGN", MemberErrorCode.MEMBER_NICKNAME_INVALID),
                    Arguments.of("카테고리 코드 공백", "test_member", "", MemberErrorCode.MEMBER_CATEGORY_CODE_INVALID),
                    Arguments.of("카테고리 코드 10자 초과", "test_member", "a".repeat(11), MemberErrorCode.MEMBER_CATEGORY_CODE_INVALID)
            );
        }
    }

}