package com.jk.amazon2.posting.service;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 요청 속도 제한을 관리하는 Rate Limiter
 * Semaphore와 AtomicLong을 사용하여 지정된 시간 간격으로 요청 허용
 */
public class RateLimiter {

    private final long refillIntervalMs; // 요청 간격 (밀리초)
    private final Semaphore semaphore;
    private final AtomicLong lastRefillTime;

    /**
     * RateLimiter 인스턴스 생성
     *
     * @param refillIntervalMs 요청 간격 (밀리초). 예: 2000 = 2초에 1개 요청
     */
    public RateLimiter(long refillIntervalMs) {
        this.refillIntervalMs = refillIntervalMs;
        this.semaphore = new Semaphore(1);
        this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * 요청 토큰 획득
     * 지정된 시간 간격이 경과하지 않았으면 대기
     *
     * @throws RuntimeException 인터럽트된 경우
     */
    public void acquire() {
        refillIfNeeded();
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Rate limiter interrupted", e);
        }
    }

    /**
     * 필요시 토큰 리필
     * 마지막 리필 시간으로부터 지정된 간격이 경과했으면 토큰 추가
     */
    private void refillIfNeeded() {
        long now = System.currentTimeMillis();
        long lastRefill = lastRefillTime.get();

        if (now - lastRefill >= refillIntervalMs) {
            if (lastRefillTime.compareAndSet(lastRefill, now)) {
                semaphore.release();
            }
        }
    }
}
