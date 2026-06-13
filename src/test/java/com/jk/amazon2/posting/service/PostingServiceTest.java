package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.Posting;
import com.jk.amazon2.posting.repository.PostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostingServiceTest {

    @Mock
    private PostingRepository postingRepository;

    private PostingService postingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        postingService = new PostingService(postingRepository);
    }

    @Test
    void testSavePosting_NewRecord() {
        Long memberId = 1L;
        LocalDate weekStart = LocalDate.of(2026, 6, 9);

        when(postingRepository.findByMemberIdAndWeekStartDateWithLock(memberId, weekStart))
            .thenReturn(Optional.empty());

        postingService.savePosting(memberId, weekStart, 1, 2, 3, 4, 5, 6, 7, "admin");

        verify(postingRepository, times(1)).save(any(Posting.class));
    }

    @Test
    void testSavePosting_UpdateExisting() {
        Long memberId = 1L;
        LocalDate weekStart = LocalDate.of(2026, 6, 9);
        Posting existing = new Posting(memberId, weekStart, 1, 1, 1, 1, 1, 1, 1, "admin");

        when(postingRepository.findByMemberIdAndWeekStartDateWithLock(memberId, weekStart))
            .thenReturn(Optional.of(existing));

        postingService.savePosting(memberId, weekStart, 5, 5, 5, 5, 5, 5, 5, "admin");

        assertEquals(5, existing.getMon());
    }
}
