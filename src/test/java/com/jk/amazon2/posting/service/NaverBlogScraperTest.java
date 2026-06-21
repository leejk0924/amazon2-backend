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

        // Then: 네트워크 실패, HTTP 오류, 또는 글 없음(0) 중 하나
        // - 블로그가 없거나 해당 날짜에 글이 없으면 category_title pcol2 요소가 없어 Success(0) 반환
        assertThat(result).satisfiesAnyOf(
                r -> {
                    assertThat(r).isInstanceOf(ScrapingResult.Failure.class);
                    assertThat(((ScrapingResult.Failure<Integer>) r).type()).isIn(
                            ScrapingResult.FailureType.NETWORK_ERROR,
                            ScrapingResult.FailureType.HTTP_ERROR
                    );
                },
                r -> {
                    assertThat(r).isInstanceOf(ScrapingResult.Success.class);
                    assertThat(((ScrapingResult.Success<Integer>) r).value()).isEqualTo(0);
                }
        );
    }
}
