package com.jk.amazon2.posting.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NaverBlogScraperTest {

    private NaverBlogScraper scraper;

    @BeforeEach
    void setUp() {
        scraper = new NaverBlogScraper();
    }

    @Test
    void testScraperIsCreated() {
        // Given & When & Then
        assertThat(scraper).isNotNull();
    }

    @Test
    void testScrapeInvalidBlogIdReturnsNetworkOrHttpFailure() {
        // Given
        String invalidBlogId = "invalid_blog_id_that_does_not_exist_xyz";
        LocalDate date = LocalDate.now();

        // When
        ScrapingResult<Integer> result = scraper.scrapePostingCount(invalidBlogId, date);

        // Then: 네트워크 실패 또는 HTTP 오류 또는 파싱 오류 중 하나
        assertThat(result).isInstanceOf(ScrapingResult.Failure.class);
        ScrapingResult.Failure<Integer> failure = (ScrapingResult.Failure<Integer>) result;
        assertThat(failure.type()).isIn(
                ScrapingResult.FailureType.NETWORK_ERROR,
                ScrapingResult.FailureType.HTTP_ERROR,
                ScrapingResult.FailureType.PARSING_ERROR
        );
    }
}
