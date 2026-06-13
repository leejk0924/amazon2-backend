package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.entity.PostingDeadLetter;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import com.jk.amazon2.posting.repository.PostingDeadLetterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 포스팅 처리 중 발생한 에러를 관리하고,
 * 재시도 로직 및 Dead Letter Queue 처리를 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorHandler {

    private static final int MAX_RETRY_COUNT = 3;

    private final PostingErrorRepository errorRepository;
    private final PostingDeadLetterRepository deadLetterRepository;

    /**
     * 에러를 로깅하고 PostingError 엔티티로 저장
     *
     * @param memberId 회원 ID
     * @param targetDate 대상 날짜
     * @param dayOfWeek 요일 (e.g., "mon", "tue")
     * @param cause 발생한 예외
     */
    @Transactional
    public void handleError(Long memberId, LocalDate targetDate, String dayOfWeek, Exception cause) {
        PostingError error = new PostingError(
            memberId,
            targetDate,
            dayOfWeek,
            cause.getMessage(),
            1
        );

        errorRepository.save(error);
        log.warn("Error logged - member={}, date={}, day={}, error={}",
            memberId, targetDate, dayOfWeek, cause.getMessage());
    }

    /**
     * 에러의 재시도 횟수를 증가시키고,
     * MAX_RETRY_COUNT에 도달하면 Dead Letter로 이동
     *
     * @param error 처리할 PostingError 엔티티
     */
    @Transactional
    public void handleRetry(PostingError error) {
        error.incrementRetryCount();

        if (error.getRetryCount() >= MAX_RETRY_COUNT) {
            moveToDeadLetter(error);
            errorRepository.delete(error);
        } else {
            errorRepository.save(error);
        }
    }

    /**
     * PostingError를 PostingDeadLetter로 이동
     *
     * @param error Dead Letter로 이동할 PostingError
     */
    private void moveToDeadLetter(PostingError error) {
        PostingDeadLetter deadLetter = new PostingDeadLetter(
            error.getMemberId(),
            error.getTargetDate(),
            error.getDayOfWeek(),
            error.getErrorMessage()
        );

        deadLetterRepository.save(deadLetter);
        log.error("Moved to dead letter - member={}, date={}, day={}",
            error.getMemberId(), error.getTargetDate(), error.getDayOfWeek());
    }
}
