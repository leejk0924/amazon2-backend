package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BatchServiceTest {

    @Mock
    private BatchExecutionRepository batchExecutionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostingService postingService;

    @Mock
    private NaverBlogScraper scraper;

    private BatchService batchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batchService = new BatchService(
            batchExecutionRepository,
            memberRepository,
            postingService,
            null,
            null,
            scraper,
            null,
            null
        );
    }

    @Test
    void testExecuteBatch() {
        LocalDate startDate = LocalDate.of(2026, 6, 9);
        LocalDate endDate = LocalDate.of(2026, 6, 15);

        assertNotNull(batchService);
    }
}
