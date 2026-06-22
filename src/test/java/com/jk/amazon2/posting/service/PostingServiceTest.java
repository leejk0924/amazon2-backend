package com.jk.amazon2.posting.service;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.member.repository.MemberRepository;
import com.jk.amazon2.posting.dto.PostingResponse;
import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.repository.PostingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    @Mock
    private PostingRepository postingRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private PostingService postingService;

    @Test
    @DisplayName("신규 포스팅 저장")
    void savePosting_신규_레코드_저장() {
        // Given
        Long memberId = 1L;
        LocalDate weekStart = LocalDate.of(2026, 6, 9);
        when(postingRepository.findByMemberIdAndWeekStartDateWithLock(memberId, weekStart))
            .thenReturn(Optional.empty());

        // When
        postingService.savePosting(memberId, weekStart, 1, 2, 3, 4, 5, 6, 7, "admin");

        // Then
        verify(postingRepository, times(1)).save(any(Posting.class));
    }

    @Test
    @DisplayName("기존 포스팅 업데이트")
    void savePosting_기존_레코드_업데이트() {
        // Given
        Long memberId = 1L;
        LocalDate weekStart = LocalDate.of(2026, 6, 9);
        Posting existing = new Posting(memberId, weekStart, 1, 1, 1, 1, 1, 1, 1, "admin");
        when(postingRepository.findByMemberIdAndWeekStartDateWithLock(memberId, weekStart))
            .thenReturn(Optional.of(existing));

        // When
        postingService.savePosting(memberId, weekStart, 5, 5, 5, 5, 5, 5, 5, "admin");

        // Then
        assertThat(existing.getMon()).isEqualTo(5);
        assertThat(existing.getTue()).isEqualTo(5);
    }

    @Test
    @DisplayName("포스팅이 있는 멤버는 실제 값으로 반환")
    void getPostings_포스팅_있는_멤버_실제값_반환() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        Pageable pageable = PageRequest.of(0, 10);
        Member member = Member.of("testUser", "test-name", "CAT01");
        ReflectionTestUtils.setField(member, "id", 1L);
        Posting posting = new Posting(1L, startDate, 1, 2, 3, 4, 5, 6, 7, "admin");
        Page<Member> memberPage = new PageImpl<>(List.of(member), pageable, 1);

        when(memberRepository.findActiveMembers(eq(null), eq(pageable))).thenReturn(memberPage);
        when(postingRepository.findAllByMemberIdsAndDateRange(anyCollection(), eq(startDate), eq(startDate)))
            .thenReturn(List.of(posting));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, null, null, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        PostingResponse.PostingDto dto = result.getContent().get(0);
        assertThat(dto.weekStartDate()).isEqualTo(startDate);
        assertThat(dto.memberNickname()).isEqualTo("testUser");
        assertThat(dto.memberName()).isEqualTo("test-name");
        assertThat(dto.mon()).isEqualTo(1);
    }

    @Test
    @DisplayName("포스팅이 없는 멤버는 0으로 반환")
    void getPostings_포스팅_없는_멤버_0으로_반환() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        Pageable pageable = PageRequest.of(0, 10);
        Member member = Member.of("noPostUser", "무포스팅", "CAT01");
        ReflectionTestUtils.setField(member, "id", 1L);
        Page<Member> memberPage = new PageImpl<>(List.of(member), pageable, 1);

        when(memberRepository.findActiveMembers(eq(null), eq(pageable))).thenReturn(memberPage);
        when(postingRepository.findAllByMemberIdsAndDateRange(anyCollection(), eq(startDate), eq(startDate)))
            .thenReturn(List.of());

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, null, null, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        PostingResponse.PostingDto dto = result.getContent().get(0);
        assertThat(dto.memberNickname()).isEqualTo("noPostUser");
        assertThat(dto.weekStartDate()).isEqualTo(startDate);
        assertThat(dto.mon()).isZero();
        assertThat(dto.tue()).isZero();
        assertThat(dto.wed()).isZero();
        assertThat(dto.thu()).isZero();
        assertThat(dto.fri()).isZero();
        assertThat(dto.sat()).isZero();
        assertThat(dto.sun()).isZero();
    }

    @Test
    @DisplayName("memberId 전달 시 특정 멤버만 필터링")
    void getPostings_memberId_필터링() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        Long memberId = 2L;
        Pageable pageable = PageRequest.of(0, 10);
        Member member = Member.of("targetUser", "test-name", "CAT01");
        ReflectionTestUtils.setField(member, "id", memberId);
        Posting posting = new Posting(memberId, startDate, 3, 3, 3, 3, 3, 3, 3, "admin");
        Page<Member> memberPage = new PageImpl<>(List.of(member), pageable, 1);

        when(memberRepository.findActiveMembers(eq(memberId), eq(pageable))).thenReturn(memberPage);
        when(postingRepository.findAllByMemberIdsAndDateRange(anyCollection(), eq(startDate), eq(startDate)))
            .thenReturn(List.of(posting));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, null, memberId, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).memberId()).isEqualTo(memberId);
        assertThat(result.getContent().get(0).memberNickname()).isEqualTo("targetUser");
        assertThat(result.getContent().get(0).memberName()).isEqualTo("test-name");
    }

    @Test
    @DisplayName("활성 멤버가 없으면 빈 페이지 반환")
    void getPostings_활성멤버_없으면_빈_페이지_반환() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        Pageable pageable = PageRequest.of(0, 10);
        when(memberRepository.findActiveMembers(eq(null), eq(pageable))).thenReturn(Page.empty(pageable));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, null, null, pageable);

        // Then
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(postingRepository);
    }
}
