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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

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
    @DisplayName("날짜 기준 포스팅 목록 조회")
    void getPostings_날짜_기준_페이징_조회() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        Pageable pageable = PageRequest.of(0, 10);
        Posting posting = new Posting(1L, startDate, 1, 2, 3, 4, 5, 6, 7, "admin");
        Page<Posting> postingPage = new PageImpl<>(List.of(posting), pageable, 1);
        Member member = Member.of("testUser", "CAT01");
        ReflectionTestUtils.setField(member, "id", 1L);
        when(postingRepository.findAllByWeekStartDate(startDate, pageable)).thenReturn(postingPage);
        when(memberRepository.findAllById(anyCollection())).thenReturn(List.of(member));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        PostingResponse.PostingDto dto = result.getContent().get(0);
        assertThat(dto.memberId()).isEqualTo(1L);
        assertThat(dto.memberNickname()).isEqualTo("testUser");
        assertThat(dto.weekStartDate()).isEqualTo(startDate);
        assertThat(dto.mon()).isEqualTo(1);
        assertThat(dto.sun()).isEqualTo(7);
    }

    @Test
    @DisplayName("해당 날짜 포스팅 없으면 빈 페이지 반환")
    void getPostings_데이터_없으면_빈_페이지_반환() {
        // Given
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        Pageable pageable = PageRequest.of(0, 10);
        when(postingRepository.findAllByWeekStartDate(startDate, pageable)).thenReturn(Page.empty(pageable));

        // When
        Page<PostingResponse.PostingDto> result = postingService.getPostings(startDate, pageable);

        // Then
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isZero();
    }
}