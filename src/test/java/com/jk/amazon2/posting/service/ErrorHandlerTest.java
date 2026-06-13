package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.entity.PostingDeadLetter;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import com.jk.amazon2.posting.repository.PostingDeadLetterRepository;
import com.jk.amazon2.posting.exception.ScrapingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ErrorHandler 서비스의 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ErrorHandlerTest {

    @Mock
    private PostingErrorRepository errorRepository;

    @Mock
    private PostingDeadLetterRepository deadLetterRepository;

    @InjectMocks
    private ErrorHandler errorHandler;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final LocalDate TEST_DATE = LocalDate.now();
    private static final String TEST_DAY_OF_WEEK = "mon";
    private static final String TEST_ERROR_MESSAGE = "test error message";

    @BeforeEach
    void setUp() {
        // MockitoExtension이 자동으로 @Mock, @InjectMocks를 초기화
    }

    /**
     * handleError 메서드가 에러를 저장하는지 검증
     */
    @Test
    void testHandleErrorCreatesErrorLog() {
        Exception cause = new ScrapingException(TEST_ERROR_MESSAGE);

        errorHandler.handleError(TEST_MEMBER_ID, TEST_DATE, TEST_DAY_OF_WEEK, cause);

        verify(errorRepository, times(1)).save(any(PostingError.class));
    }

    /**
     * handleRetry 메서드가 재시도 횟수를 증가시키는지 검증
     */
    @Test
    void testHandleRetryIncrementsRetryCount() {
        PostingError error = new PostingError(TEST_MEMBER_ID, TEST_DATE, TEST_DAY_OF_WEEK, TEST_ERROR_MESSAGE, 1);

        errorHandler.handleRetry(error);

        verify(errorRepository, times(1)).save(any(PostingError.class));
        verify(deadLetterRepository, never()).save(any(PostingDeadLetter.class));
    }

    /**
     * 재시도 횟수가 MAX_RETRY_COUNT에 도달하면 Dead Letter로 이동하는지 검증
     */
    @Test
    void testRetry3TimesMovesToDeadLetter() {
        // retryCount가 이미 2인 상태로 시작 (incrementRetryCount 후 3이 됨)
        PostingError error = new PostingError(TEST_MEMBER_ID, TEST_DATE, TEST_DAY_OF_WEEK, TEST_ERROR_MESSAGE, 2);

        errorHandler.handleRetry(error);

        // Dead Letter로 이동되어야 함
        verify(deadLetterRepository, times(1)).save(any(PostingDeadLetter.class));
        verify(errorRepository, times(1)).delete(any(PostingError.class));
    }

    /**
     * Dead Letter 로 이동 시 원본 에러 레코드가 삭제되는지 검증
     */
    @Test
    void testErrorDeletedWhenMovedToDeadLetter() {
        PostingError error = new PostingError(TEST_MEMBER_ID, TEST_DATE, TEST_DAY_OF_WEEK, TEST_ERROR_MESSAGE, 2);

        errorHandler.handleRetry(error);

        verify(errorRepository, times(1)).delete(error);
    }
}
