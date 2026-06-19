package com.jk.amazon2.posting.service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 요청 속도 제한을 관리하는 Rate Limiter
 * 첫 번째 요청은 즉시 허용, 이후 요청은 지정된 간격만큼 대기
 */
public class RateLimiter {

    private final long intervalMs;
    // 다음 요청이 가능한 시각 (초기값 = 현재 → 첫 요청 즉시 허용)
    private final AtomicLong nextAvailableAt;

    public RateLimiter(long intervalMs) {
        this.intervalMs = intervalMs;
        this.nextAvailableAt = new AtomicLong(System.currentTimeMillis());
    }

    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long waitMs = nextAvailableAt.get() - now;

        if (waitMs > 0) {
            try {
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Rate limiter interrupted", e);
            }
        }
        nextAvailableAt.set(System.currentTimeMillis() + intervalMs);
    }
}
