package com.jk.amazon2.controller;

import com.jk.amazon2.controller.dto.MemberRequest;
import com.jk.amazon2.controller.dto.MemberResponse;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.ErrorCode;
import com.jk.amazon2.exception.MemberErrorCode;
import com.jk.amazon2.exception.RestApiException;
import com.jk.amazon2.service.MemberService;
import com.jk.amazon2.service.dto.MemberCommand;
import com.jk.amazon2.service.dto.MemberResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class MemberControllerTest {
    @Mock
    private MemberService memberService;

    private MemberController memberController;

    @BeforeEach
    void setUp() {
        memberController = new MemberController(memberService);
    }

    @Nested
    @DisplayName("Member 생성 - 단위 테스트")
    class CreateMember {
        @Test
        @DisplayName("회원 생성 성공")
        void createMember_success() {
            // given
            long id = 1L;
            String nickname = "test_member";
            String categoryCode = "DEV";

            var request = new MemberRequest.MemberCreateDto(nickname, categoryCode);

            var savedMember = MemberResult.Detail.of(id, nickname, categoryCode, LocalDateTime.now(), false);
            given(memberService.create(any(MemberCommand.Create.class)))
                    .willReturn(savedMember);

            // when
            ResponseEntity<MemberResponse.MemberCreateDto> response = memberController.createMember(request);
        }

        @DisplayName("회원 생성 실패 케이스 [fail]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideCreateMemberFailureCases")
        void createMember_fail(String scenario, String nickname, String categoryCode, ErrorCode errorCode) {
            // given
            var request = new MemberRequest.MemberCreateDto(nickname, categoryCode);

            // 서비스 로직 에러인 경우에만 Mocking (유효성 검사 실패는 서비스 호출 전 발생)
            if (errorCode == MemberErrorCode.MEMBER_NICKNAME_ALREADY_EXISTS || errorCode == CategoryErrorCode.CATEGORY_NOT_FOUND) {
                given(memberService.create(any(MemberCommand.Create.class)))
                        .willThrow(new RestApiException(errorCode));
            }

            // when & then
            assertThatThrownBy(() -> memberController.createMember(request))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(errorCode.getMessage());
        }

        private static Stream<Arguments> provideCreateMemberFailureCases() {
            return Stream.of(
                    Arguments.of("닉네임 중복", "test_member", "DEV", MemberErrorCode.MEMBER_NICKNAME_ALREADY_EXISTS),
                    Arguments.of("존재하지 않는 카테고리", "test_member", "NON_EXISTENT", CategoryErrorCode.CATEGORY_NOT_FOUND),
                    Arguments.of("닉네임 공백", "", "DEV", MemberErrorCode.MEMBER_NICKNAME_INVALID),
                    Arguments.of("닉네임 null", null, "DEV", MemberErrorCode.MEMBER_NICKNAME_INVALID),
                    Arguments.of("닉네임 50자 초과", "a".repeat(51), "DEV", MemberErrorCode.MEMBER_NICKNAME_INVALID)
            );
        }
    }

    @Nested
    @DisplayName("Member 조회 - 단위 테스트")
    class GetMember {
        @Test
        @DisplayName("회원 조회 성공")
        void getMember_success() {
            // given
            Long id = 1L;
            var result = MemberResult.Detail.of(id, "test_member", "DEV", LocalDateTime.now(), false);
            given(memberService.findById(id))
                    .willReturn(result);

            // when
            ResponseEntity<MemberResponse.MemberDetailDto> response = memberController.getMember(id);

            // then
            assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().id()).isEqualTo(id);
            assertThat(response.getBody().nickname()).isEqualTo("test_member");
            assertThat(response.getBody().categoryCode()).isEqualTo("DEV");
            assertThat(response.getBody().status()).isEqualTo("active");
        }

        @Test
        @DisplayName("회원 조회 실패 - 존재하지 않는 회원")
        void getMember_fail_not_found() {
            // given
            Long id = 999L;
            given(memberService.findById(id))
                    .willThrow(new RestApiException(MemberErrorCode.MEMBER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> memberController.getMember(id))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

}
