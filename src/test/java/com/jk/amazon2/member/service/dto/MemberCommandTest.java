package com.jk.amazon2.member.service.dto;

import com.jk.amazon2.member.dto.MemberRequest;
import com.jk.amazon2.member.exception.MemberErrorCode;
import com.jk.amazon2.common.exception.RestApiException;
import com.jk.amazon2.member.dto.MemberCommand;
import com.jk.amazon2.member.dto.MemberResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.of;

@ExtendWith(MockitoExtension.class)
class MemberCommandTest {
    @Nested
    @DisplayName("MemberCommand.Create 테스트")
    class MemberCommand_Create {
        @Test
        @DisplayName("MemberCommand.Create 생성 성공 [success]")
        void create_success() {
            // given
            String nickname = "tester";
            String categoryCode = "DEV";

            // when
            MemberCommand.Create command = MemberCommand.Create.of(nickname, null, categoryCode);

            // then
            assertSoftly(softly -> {
                softly.assertThat(command.getNickname()).isEqualTo(nickname);
                softly.assertThat(command.getCategoryCode()).isEqualTo(categoryCode);
            });
        }

        @DisplayName("닉네임 유효성 검사 실패 케이스 [fail]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideInvalidNicknames")
        void create_fail_invalid_nickname(
                String scenario,
                String invalidNickname
        ) {
            // when & then
            assertThatThrownBy(() -> MemberCommand.Create.of(invalidNickname, null, "DEV"))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NICKNAME_INVALID.getMessage());
        }

        private static Stream<Arguments> provideInvalidNicknames() {
            return Stream.of(
                    of("nickname이 null인 경우", null),
                    of("nickname이 빈 문자열인 경우", ""),
                    of("nickname이 공백인 경우", "   "),
                    of("nickname이 50자를 초과하는 경우", "a".repeat(51))
            );
        }

        @Test
        @DisplayName("name이 20자를 초과하면 생성 실패 [fail]")
        void create_fail_invalid_name() {
            // when & then
            assertThatThrownBy(() -> MemberCommand.Create.of("tester", "a".repeat(21), "DEV"))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NAME_INVALID.getMessage());
        }

        @Test
        @DisplayName("name이 null이면 생성 성공 (선택값) [success]")
        void create_success_name_null() {
            // when
            MemberCommand.Create command = MemberCommand.Create.of("tester", null, "DEV");

            // then
            assertThat(command.getName()).isNull();
        }

        @Test
        @DisplayName("name이 20자이면 생성 성공 [success]")
        void create_success_name_max_length() {
            // when
            MemberCommand.Create command = MemberCommand.Create.of("tester", "a".repeat(20), "DEV");

            // then
            assertThat(command.getName()).isEqualTo("a".repeat(20));
        }
    }

    @Nested
    @DisplayName("MemberCommand.Update 테스트")
    class MemberCommand_Update {
        @DisplayName("MemberCommand.Update 생성 성공 케이스 [success]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideValidUpdateCommands")
        void update_success(
                String scenario,
                String nickname,
                String categoryCode
        ) {
            // when
            MemberCommand.Update command = MemberCommand.Update.of(1L, nickname, null, categoryCode);

            // then
            assertThat(command).isNotNull();
            assertSoftly(softly -> {
                softly.assertThat(command.getNickname()).isEqualTo(nickname);
                softly.assertThat(command.getCategoryCode()).isEqualTo(categoryCode);
            });
        }

        private static Stream<Arguments> provideValidUpdateCommands() {
            return Stream.of(
                    of("기본 성공", "updatedNickname", "DESIGN"),
                    of("categoryCode null", "updatedNickname", null),
                    of("categoryCode 10자", "updatedNickname", "a".repeat(10))
            );
        }

        @DisplayName("닉네임 유효성 검사 실패 케이스 [fail]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideInvalidUpdateNicknames")
        void update_fail_invalid_nickname(
                String scenario,
                String invalidNickname,
                String categoryCode
        ) {
            // when & then
            assertThatThrownBy(() -> MemberCommand.Update.of(1L, invalidNickname, null, categoryCode))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NICKNAME_INVALID.getMessage());
        }

        private static Stream<Arguments> provideInvalidUpdateNicknames() {
            return Stream.of(
                    of("nickname이 null인 경우", null, "DESIGN"),
                    of("nickname이 빈 문자열인 경우", "", "DESIGN"),
                    of("nickname이 공백인 경우", "   ", "DESIGN"),
                    of("nickname이 50자를 초과하는 경우", "a".repeat(51), "DESIGN")
            );
        }

        @DisplayName("카테고리 코드 유효성 검사 실패 케이스 [fail]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideInvalidUpdateCategoryCodes")
        void update_fail_invalid_categoryCode(
                String scenario,
                String nickname,
                String invalidCategoryCode
        ) {
            // when & then
            assertThatThrownBy(() -> MemberCommand.Update.of(1L, nickname, null, invalidCategoryCode))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_CATEGORY_CODE_INVALID.getMessage());
        }

        private static Stream<Arguments> provideInvalidUpdateCategoryCodes() {
            return Stream.of(
                    of("categoryCode가 빈 문자열인 경우", "tester", ""),
                    of("categoryCode가 공백인 경우", "tester", "   "),
                    of("categoryCode가 10자를 초과하는 경우", "tester", "a".repeat(11))
            );
        }

        @Test
        @DisplayName("name이 20자를 초과하면 수정 실패 [fail]")
        void update_fail_invalid_name() {
            // when & then
            assertThatThrownBy(() -> MemberCommand.Update.of(1L, "tester", "a".repeat(21), "DEV"))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NAME_INVALID.getMessage());
        }

        @Test
        @DisplayName("name이 null이면 수정 성공 (선택값) [success]")
        void update_success_name_null() {
            // when
            MemberCommand.Update command = MemberCommand.Update.of(1L, "tester", null, "DEV");

            // then
            assertThat(command.getName()).isNull();
        }
    }

    @Nested
    @DisplayName("MemberCommand.Search 테스트")
    class MemberCommand_Search {
        @DisplayName("status를 deleted boolean으로 변환 [success]")
        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("provideStatusConversions")
        void search_from_with_status_conversion(
                String scenario,
                String inputStatus,
                Boolean expectedDeleted
        ) {
            // given
            var condition = new MemberRequest.MemberSearchCondition(null, null, inputStatus);

            // when
            MemberCommand.Search command = MemberCommand.Search.from(condition);

            // then
            assertThat(command.getDeleted()).isEqualTo(expectedDeleted);
        }

        private static Stream<Arguments> provideStatusConversions() {
            return Stream.of(
                    of("active 상태 변환", "active", false),
                    of("deleted 상태 변환", "deleted", true),
                    of("상태 지정 없음", null, null)
            );
        }
    }
}
