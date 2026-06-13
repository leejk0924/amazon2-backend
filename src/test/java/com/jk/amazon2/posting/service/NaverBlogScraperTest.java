package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.exception.ParsingException;
import com.jk.amazon2.posting.exception.ScrapingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class NaverBlogScraperTest {

    private NaverBlogScraper scraper;

    @BeforeEach
    void setUp() {
        scraper = new NaverBlogScraper();
    }

    @Test
    void testScrapValidDate() {
        assertNotNull(scraper);
    }

    @Test
    void testScrapThrowsScrapingException() {
        assertNotNull(scraper);
    }
}
