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
    @DisplayName("startDate만 전달하면 정확 일치 조회 (effectiveEndDate = startDate)")
    void getPostings_startDate만_전달시_정확일치_조회() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        Pageable pageable = PageRequest.of(0, 10);
        Posting posting = new Posting(1L, startDate, 1, 2, 3, 4, 5, 6, 7, "admin");
        Page<Posting> postingPage = new PageImpl<>(List.of(posting), pageable, 1);
        Member member = Member.of("testUser", null, "CAT01");
        ReflectionTestUtils.setField(member, "id", 1L);

        when(postingRepository.findAllBySearchCondition(eq(startDate), eq(startDate), eq(null), eq(pageable)))
            .thenReturn(postingPage);
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(member));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, null, null, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        PostingResponse.PostingDto dto = result.getContent().get(0);
        assertThat(dto.weekStartDate()).isEqualTo(startDate);
        assertThat(dto.memberNickname()).isEqualTo("testUser");
        verify(postingRepository).findAllBySearchCondition(startDate, startDate, null, pageable);
    }

    @Test
    @DisplayName("startDate + endDate 전달 시 기간 범위 조회")
    void getPostings_기간범위_조회() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 2);
        LocalDate endDate = LocalDate.of(2026, 6, 23);
        Pageable pageable = PageRequest.of(0, 10);
        Posting posting1 = new Posting(1L, LocalDate.of(2026, 6, 2), 1, 0, 0, 0, 0, 0, 0, "admin");
        Posting posting2 = new Posting(1L, LocalDate.of(2026, 6, 9), 0, 1, 0, 0, 0, 0, 0, "admin");
        Page<Posting> postingPage = new PageImpl<>(List.of(posting1, posting2), pageable, 2);
        Member member = Member.of("testUser", null, "CAT01");
        ReflectionTestUtils.setField(member, "id", 1L);

        when(postingRepository.findAllBySearchCondition(eq(startDate), eq(endDate), eq(null), eq(pageable)))
            .thenReturn(postingPage);
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(member));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, endDate, null, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(postingRepository).findAllBySearchCondition(startDate, endDate, null, pageable);
    }

    @Test
    @DisplayName("memberId 전달 시 특정 멤버만 필터링")
    void getPostings_memberId_필터링() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        Long memberId = 2L;
        Pageable pageable = PageRequest.of(0, 10);
        Posting posting = new Posting(memberId, startDate, 3, 3, 3, 3, 3, 3, 3, "admin");
        Page<Posting> postingPage = new PageImpl<>(List.of(posting), pageable, 1);
        Member member = Member.of("targetUser", null, "CAT01");
        ReflectionTestUtils.setField(member, "id", memberId);

        when(postingRepository.findAllBySearchCondition(eq(startDate), eq(startDate), eq(memberId), eq(pageable)))
            .thenReturn(postingPage);
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(member));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, null, memberId, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).memberId()).isEqualTo(memberId);
        assertThat(result.getContent().get(0).memberNickname()).isEqualTo("targetUser");
        verify(postingRepository).findAllBySearchCondition(startDate, startDate, memberId, pageable);
    }

    @Test
    @DisplayName("조건 없으면 빈 페이지 반환")
    void getPostings_데이터_없으면_빈_페이지_반환() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        Pageable pageable = PageRequest.of(0, 10);
        when(postingRepository.findAllBySearchCondition(eq(startDate), eq(startDate), eq(null), eq(pageable)))
            .thenReturn(Page.empty(pageable));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, null, null, pageable);

        // Then
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isZero();
    }
}
