package com.jk.amazon2.member.service;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.service.MemberService;
import com.jk.amazon2.category.exception.CategoryErrorCode;
import com.jk.amazon2.member.exception.MemberErrorCode;
import com.jk.amazon2.common.exception.RestApiException;
import com.jk.amazon2.common.port.CategoryValidationPort;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.member.dto.MemberCommand;
import com.jk.amazon2.member.dto.MemberResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CategoryValidationPort categoryValidationPort;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, categoryValidationPort);
    }

    @Nested
    @DisplayName("Member 생성 - 단위 테스트")
    class CreateMember {
        @DisplayName("유저 생성 시, 입력된 정보가 엔티티로 변환되어 저장 [success]")
        @Test
        void member_create_success() {
            // given
            Long id = 1L;
            String nickname = "test";
            String name = "test-name";
            String categoryCode = "TEST";

            var inputMember = MemberCommand.Create.of(nickname, name, categoryCode);

            given(memberRepository.existsByNickname(any(String.class)))
                    .willReturn(false);
            given(memberRepository.save(any(Member.class)))
                    .willAnswer(invocation -> {
                        Member memberToSave = invocation.getArgument(0);
                        ReflectionTestUtils.setField(memberToSave, "id", id);
                        return memberToSave;
                    });

            // when
            MemberResult.Detail savedMember = memberService.create(inputMember);
            ReflectionTestUtils.setField(savedMember, "id", id);

            // then
            assertThat(savedMember).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(savedMember.getNickname()).isEqualTo(nickname);
                softly.assertThat(savedMember.getName()).isEqualTo(name);
                softly.assertThat(savedMember.getCategoryCode()).isEqualTo(categoryCode);
                softly.assertThat(savedMember.getId()).isEqualTo(id);
            });

            ArgumentCaptor<Member> commandCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(commandCaptor.capture());
            Member capturedCommand = commandCaptor.getValue();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedCommand.getId()).as("유저의 id 검증").isEqualTo(id);
                softly.assertThat(capturedCommand.getNickname()).as("유저명 검증").isEqualTo(nickname);
                softly.assertThat(capturedCommand.getName()).as("이름 검증").isEqualTo(name);
                softly.assertThat(capturedCommand.getCategoryCode()).as("카테고리 코드 검증").isEqualTo(categoryCode);
            });
        }

        @DisplayName("유저 생성 시, 존재하지 않는 카테고리 코드를 입력하면 실패 [fail]")
        @Test
        void member_create_fail_category_not_found() {
            // given
            String nickname = "test";
            String categoryCode = "NON_EXISTENT_CODE";
            var inputMember = MemberCommand.Create.of(nickname, "test-name", categoryCode);

            doThrow(new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND))
                    .when(categoryValidationPort).validateCategoryExists(categoryCode);

            // when & then
            assertThatThrownBy(() -> memberService.create(inputMember))
                    .isInstanceOf(RestApiException.class)
                    .hasMessage(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage());
        }

        @DisplayName("유저 생성 시, 이미 존재하는 닉네임이면 실패 [fail]")
        @Test
        void member_create_fail_nickname_duplicate() {
            // given
            String nickname = "duplicate_user";
            String categoryCode = "TEST";

            var inputMember = MemberCommand.Create.of(nickname, "test-name", categoryCode);

            given(memberRepository.existsByNickname(any(String.class)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.create(inputMember))
                    .isInstanceOf(RestApiException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NICKNAME_ALREADY_EXISTS.getMessage());
        }
    }

    @Nested
    @DisplayName("Member 업데이트 - 단위 테스트")
    class UpdateMember {
        @DisplayName("회원 정보 업데이트 성공 [success]")
        @Test
        void update_success() {
            // given
            String currentNickname = "test_member";
            String newNickname = "updated_member";
            String newName = "updated-name";
            String newCategoryCode = "UPDATED";
            Member member = Member.of(currentNickname, "test-name", "DEV");

            var updateCommand = MemberCommand.Update.of(currentNickname, newNickname, newName, newCategoryCode);
            given(memberRepository.findByNickname(currentNickname))
                    .willReturn(Optional.of(member));

            // when
            MemberResult.Update result = memberService.update(updateCommand);

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.nickname()).isEqualTo(newNickname);
                softly.assertThat(result.name()).isEqualTo(newName);
                softly.assertThat(result.categoryCode()).isEqualTo(newCategoryCode);
            });
            verify(categoryValidationPort).validateCategoryExists(newCategoryCode);
        }

        @DisplayName("회원 업데이트 실패 - 존재하지 않는 닉네임 [fail]")
        @Test
        void update_fail_not_found() {
            // given
            String currentNickname = "non_existent";
            var updateCommand = MemberCommand.Update.of(currentNickname, "updated_member", "updated-name", "UPDATED");

            given(memberRepository.findByNickname(currentNickname))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.update(updateCommand))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @DisplayName("회원 업데이트 실패 - 존재하지 않는 카테고리 [fail]")
        @Test
        void update_fail_category_not_found() {
            // given
            String currentNickname = "test_member";
            String newCategoryCode = "NOTEXIST";
            Member member = Member.of(currentNickname, "test-name", "DEV");
            var updateCommand = MemberCommand.Update.of(currentNickname, "updated_member", "updated-name", newCategoryCode);

            given(memberRepository.findByNickname(currentNickname))
                    .willReturn(Optional.of(member));
            doThrow(new RestApiException(CategoryErrorCode.CATEGORY_NOT_FOUND))
                    .when(categoryValidationPort).validateCategoryExists(newCategoryCode);

            // when & then
            assertThatThrownBy(() -> memberService.update(updateCommand))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(CategoryErrorCode.CATEGORY_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("Member 삭제 - 단위 테스트")
    class DeleteMember {
        @Test
        @DisplayName("회원 삭제 성공 [success]")
        void delete_success() {
            // given
            String nickname = "test_member";
            Member member = Member.of(nickname, "test-name", "DEV");
            given(memberRepository.findByNickname(nickname))
                    .willReturn(Optional.of(member));

            // when
            memberService.delete(nickname);

            // then
            assertThat(member.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("이미 삭제된 회원 재삭제 성공 - 멱등성 [success]")
        void delete_success_already_deleted() {
            // given
            String nickname = "test_member";
            Member member = Member.of(nickname, "test-name", "DEV");
            member.softDelete();
            given(memberRepository.findByNickname(nickname))
                    .willReturn(Optional.of(member));

            // when
            memberService.delete(nickname);

            // then
            assertThat(member.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("회원 삭제 실패 - 존재하지 않는 닉네임 [fail]")
        void delete_fail_not_found() {
            // given
            String nickname = "non_existent";
            given(memberRepository.findByNickname(nickname))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.delete(nickname))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("Member 조회 - 단위 테스트")
    class GetMember {
        @Test
        @DisplayName("닉네임으로 회원 조회 성공 [success]")
        void findByNickname_success() {
            // given
            Long id = 1L;
            String nickname = "test_member";
            Member member = Member.of(nickname, "test-name", "DEV");
            ReflectionTestUtils.setField(member, "id", id);
            given(memberRepository.findByNickname(nickname))
                    .willReturn(Optional.of(member));

            // when
            MemberResult.Detail result = memberService.findByNickname(nickname);

            // then
            assertThat(result).isNotNull();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getId()).isEqualTo(id);
                softly.assertThat(result.getNickname()).isEqualTo(nickname);
                softly.assertThat(result.getCategoryCode()).isEqualTo("DEV");
                softly.assertThat(result.isDeleted()).isFalse();
            });
        }

        @Test
        @DisplayName("닉네임으로 회원 조회 실패 - 존재하지 않는 닉네임 [fail]")
        void findByNickname_fail_not_found() {
            // given
            String nickname = "non_existent";
            given(memberRepository.findByNickname(nickname))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.findByNickname(nickname))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("Member 영구 삭제 - 단위 테스트")
    class HardDeleteMember {
        @Test
        @DisplayName("회원 영구 삭제 성공 [success]")
        void hardDelete_success() {
            // given
            String nickname = "test_member";
            Member member = Member.of(nickname, "test-name", "DEV");
            member.softDelete();
            given(memberRepository.findByNickname(nickname))
                    .willReturn(Optional.of(member));

            // when
            memberService.hardDelete(nickname);

            // then
            verify(memberRepository).delete(member);
        }

        @Test
        @DisplayName("회원 영구 삭제 실패 - 존재하지 않는 닉네임 [fail]")
        void hardDelete_fail_not_found() {
            // given
            String nickname = "non_existent";
            given(memberRepository.findByNickname(nickname))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> memberService.hardDelete(nickname))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("회원 영구 삭제 실패 - 소프트 삭제되지 않은 회원 [fail]")
        void hardDelete_fail_not_deleted() {
            // given
            String nickname = "test_member";
            Member member = Member.of(nickname, "test-name", "DEV");
            given(memberRepository.findByNickname(nickname))
                    .willReturn(Optional.of(member));

            // when & then
            assertThatThrownBy(() -> memberService.hardDelete(nickname))
                    .isInstanceOf(RestApiException.class)
                    .hasMessageContaining(MemberErrorCode.MEMBER_NOT_DELETED.getMessage());
        }
    }
}
