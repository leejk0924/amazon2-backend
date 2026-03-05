package com.jk.amazon2.service;

import com.jk.amazon2.entity.Category;
import com.jk.amazon2.entity.Member;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.MemberErrorCode;
import com.jk.amazon2.exception.RestApiException;
import com.jk.amazon2.repository.CategoryRepository;
import com.jk.amazon2.repository.MemberRepository;
import com.jk.amazon2.service.dto.MemberCommand;
import com.jk.amazon2.service.dto.MemberResult;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CategoryRepository categoryRepository;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, categoryRepository);

    }

    @Nested
    @DisplayName("Member 생성 - 단위 테스트")
    class CreateMember {
        @DisplayName("유저 생성 시, 일력된 정보가 엔티티로 변환되어 저장 [success]")
        @Test
        void member_create_success() {
            // given
            Long id = 1L;
            String nickname = "test";
            String categoryCode = "TEST";
            Category category = Category.of(categoryCode, "테스트카테고리", "설명");

            var inputMember = MemberCommand.Create.of(nickname, categoryCode);

            given(categoryRepository.findByCodeAndDeletedFalse(categoryCode))
                    .willReturn(Optional.of(category));
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
            SoftAssertions.assertSoftly(softly ->{
                softly.assertThat(savedMember.getNickname()).isEqualTo(nickname);
                softly.assertThat(savedMember.getCategoryCode()).isEqualTo(categoryCode);
                softly.assertThat(savedMember.getId()).isEqualTo(id);
            });

            // Verify Service Call
            ArgumentCaptor<Member> commandCaptor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(commandCaptor.capture());
            Member capturedCommand = commandCaptor.getValue();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(capturedCommand.getId()).as("유저의 id 검증").isEqualTo(id);
                softly.assertThat(capturedCommand.getNickname()).as("유저명 검증").isEqualTo(nickname);
                softly.assertThat(capturedCommand.getCategoryCode()).as("카테고리 코드 검증").isEqualTo(categoryCode);
            });
        }

        @DisplayName("유저 생성 시, 존재하지 않는 카테고리 코드를 입력하면 실패 [fail]")
        @Test
        void member_create_fail_category_not_found() {
            // given
            String nickname = "test";
            String categoryCode = "NON_EXISTENT_CODE";
            var inputMember = MemberCommand.Create.of(nickname, categoryCode);

            given(categoryRepository.findByCodeAndDeletedFalse(categoryCode)).willReturn(Optional.empty());

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

            var inputMember = MemberCommand.Create.of(nickname, categoryCode);

            given(memberRepository.existsByNickname(any(String.class)))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> memberService.create(inputMember))
                    .isInstanceOf(RestApiException.class)
                    .hasMessage(MemberErrorCode.MEMBER_NICKNAME_ALREADY_EXISTS.getMessage());
        }
    }
}
