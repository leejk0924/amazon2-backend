package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BatchServiceTest {

    @Mock
    private BatchExecutionRepository batchExecutionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NaverBlogScraper scraper;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private BatchTaskProcessor batchTaskProcessor;

    private BatchService batchService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batchService = new BatchService(
            batchExecutionRepository,
            memberRepository,
            scraper,
            rateLimiter,
            batchTaskProcessor
        );
    }

    @Test
    void testExecuteBatch() {
        assertThat(batchService).isNotNull();
    }
}
