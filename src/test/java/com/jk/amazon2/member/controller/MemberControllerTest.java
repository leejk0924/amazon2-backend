package com.jk.amazon2.member.controller;

import com.jk.amazon2.member.controller.MemberController;
import com.jk.amazon2.member.dto.MemberRequest;
import com.jk.amazon2.member.dto.MemberResponse;
import com.jk.amazon2.category.exception.CategoryErrorCode;
import com.jk.amazon2.common.exception.ErrorCode;
import com.jk.amazon2.member.exception.MemberErrorCode;
import com.jk.amazon2.common.exception.RestApiException;
import com.jk.amazon2.member.service.MemberService;
import com.jk.amazon2.member.dto.MemberCommand;
import com.jk.amazon2.member.dto.MemberResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;


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

            var request = new MemberRequest.MemberCreateDto(nickname, "test-name", categoryCode);

            var savedMember = MemberResult.Detail.of(id, nickname, "test-name", categoryCode, LocalDateTime.now(), false);
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
            var request = new MemberRequest.MemberCreateDto(nickname, "test-name", categoryCode);

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
    @DisplayName("Member 수정 - 단위 테스트")
    class UpdateMember {
        @Test
        @DisplayName("회원 수정 성공 [success]")
        void updateMember_success() {
            // given
            String nickname = "test_member";
            var request = new MemberRequest.MemberDto("updated-name", "DESIGN");
            var updateResult = MemberResult.Update.of(nickname, "updated-name", "DESIGN");

            given(memberService.update(any(MemberCommand.Update.class)))
                    .willReturn(updateResult);

            // when
            ResponseEntity<MemberResponse.MemberUpdateDto> response = memberController.updateMember(nickname, request);

            // then
            assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().nickname()).isEqualTo(nickname);
            assertThat(response.getBody().name()).isEqualTo("updated-name");
            assertThat(response.getBody().categoryCode()).isEqualTo("DESIGN");
            verify(memberService).update(any(MemberCommand.Update.class));
        }

        @DisplayName("회원 수정 실패 케이스 [fail]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideUpdateMemberFailureCases")
        void updateMember_fail(String scenario, String currentNickname, String name, MemberErrorCode errorCode) {
            // when & then
            assertThatThrownBy(() -> MemberCommand.Update.of(currentNickname, name, "DEV"))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(errorCode.getMessage());
        }

        private static Stream<Arguments> provideUpdateMemberFailureCases() {
            return Stream.of(
                    Arguments.of("currentNickname null", null, "valid-name", MemberErrorCode.MEMBER_NICKNAME_INVALID),
                    Arguments.of("currentNickname 공백", "", "valid-name", MemberErrorCode.MEMBER_NICKNAME_INVALID),
                    Arguments.of("currentNickname 50자 초과", "a".repeat(51), "valid-name", MemberErrorCode.MEMBER_NICKNAME_INVALID),
                    Arguments.of("name null", "valid_nick", null, MemberErrorCode.MEMBER_NAME_INVALID),
                    Arguments.of("name 공백", "valid_nick", "", MemberErrorCode.MEMBER_NAME_INVALID),
                    Arguments.of("name 50자 초과", "valid_nick", "a".repeat(51), MemberErrorCode.MEMBER_NAME_INVALID)
            );
        }
    }

    @Nested
    @DisplayName("Member 삭제 - 단위 테스트")
    class DeleteMember {
        @Test
        @DisplayName("회원 삭제 성공")
        void deleteMember_success() {
            // given
            String nickname = "test_member";

            // when
            ResponseEntity<Void> response = memberController.deleteMember(nickname);

            // then
            assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
            verify(memberService).delete(nickname);
        }

        @Test
        @DisplayName("회원 삭제 실패 - 존재하지 않는 닉네임")
        void deleteMember_fail_not_found() {
            // given
            String nickname = "non_existent";
            Mockito.doThrow(new RestApiException(MemberErrorCode.MEMBER_NOT_FOUND))
                    .when(memberService).delete(nickname);

            // when & then
            assertThatThrownBy(() -> memberController.deleteMember(nickname))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("Member 조회 - 단위 테스트")
    class GetMember {
        @Test
        @DisplayName("닉네임으로 회원 조회 성공")
        void getMember_success() {
            // given
            Long id = 1L;
            String nickname = "test_member";
            var result = MemberResult.Detail.of(id, nickname, "test-name", "DEV", LocalDateTime.now(), false);
            given(memberService.findByNickname(nickname))
                    .willReturn(result);

            // when
            ResponseEntity<MemberResponse.MemberDetailDto> response = memberController.getMember(nickname);

            // then
            assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().id()).isEqualTo(id);
            assertThat(response.getBody().nickname()).isEqualTo(nickname);
            assertThat(response.getBody().categoryCode()).isEqualTo("DEV");
            assertThat(response.getBody().status()).isEqualTo("active");
        }

        @Test
        @DisplayName("닉네임으로 회원 조회 실패 - 존재하지 않는 닉네임")
        void getMember_fail_not_found() {
            // given
            String nickname = "non_existent";
            given(memberService.findByNickname(nickname))
                    .willThrow(new RestApiException(MemberErrorCode.MEMBER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> memberController.getMember(nickname))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("Member 복구 - 단위 테스트")
    class RestoreMember {
        @Test
        @DisplayName("회원 복구 성공 [success]")
        void restoreMember_success() {
            // Given
            String nickname = "test_member";

            // When
            ResponseEntity<Void> response = memberController.restoreMember(nickname);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
            verify(memberService).restore(nickname);
        }

        @Test
        @DisplayName("회원 복구 실패 - 존재하지 않는 닉네임 [fail]")
        void restoreMember_fail_not_found() {
            // Given
            String nickname = "non_existent";
            doThrow(new RestApiException(MemberErrorCode.MEMBER_NOT_FOUND))
                    .when(memberService).restore(nickname);

            // When & Then
            assertThatThrownBy(() -> memberController.restoreMember(nickname))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("회원 복구 실패 - 이미 활성 상태인 회원 [fail]")
        void restoreMember_fail_already_active() {
            // Given
            String nickname = "active_member";
            doThrow(new RestApiException(MemberErrorCode.MEMBER_ALREADY_ACTIVE))
                    .when(memberService).restore(nickname);

            // When & Then
            assertThatThrownBy(() -> memberController.restoreMember(nickname))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_ALREADY_ACTIVE.getMessage());
        }
    }

    @Nested
    @DisplayName("Member 영구 삭제 - 단위 테스트")
    class HardDeleteMember {
        @Test
        @DisplayName("회원 영구 삭제 성공")
        void hardDeleteMember_success() {
            // given
            String nickname = "test_member";

            // when
            ResponseEntity<Void> response = memberController.hardDeleteMember(nickname);

            // then
            assertThat(response.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NO_CONTENT);
            verify(memberService).hardDelete(nickname);
        }

        @Test
        @DisplayName("회원 영구 삭제 실패 - 소프트 삭제되지 않은 회원")
        void hardDeleteMember_fail_not_deleted() {
            // given
            String nickname = "test_member";
            doThrow(new RestApiException(MemberErrorCode.MEMBER_NOT_DELETED))
                    .when(memberService).hardDelete(nickname);

            // when & then
            assertThatThrownBy(() -> memberController.hardDeleteMember(nickname))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_DELETED.getMessage());
        }
    }
}
