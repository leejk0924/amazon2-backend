package com.jk.amazon2.posting.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * RateLimiter 테스트 클래스
 * 2초 간격의 요청 제한 기능을 검증
 */
class RateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(2000); // 2초에 1개 요청
    }

    @Test
    void testFirstRequestAllowedImmediately() {
        // Given
        long start = System.currentTimeMillis();

        // When
        rateLimiter.acquire(); // 즉시 허용

        // Then
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 100, "첫 요청은 100ms 내에 허용되어야 함");
    }

    @Test
    void testSecondRequestWaitsTwoSeconds() {
        // Given
        long start = System.currentTimeMillis();

        // When
        rateLimiter.acquire();
        rateLimiter.acquire(); // 2초 대기

        // Then
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 1900, "두 번째 요청은 최소 1.9초 대기해야 함 (오차 고려)");
    }
}
