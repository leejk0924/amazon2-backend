package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.dto.BatchStatusResponse;
import com.jk.amazon2.posting.dto.ErrorLogDto;
import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.entity.PostingDeadLetter;
import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.posting.repository.PostingDeadLetterRepository;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MonitoringServiceTest {

    @Mock
    private BatchExecutionRepository batchExecutionRepository;

    @Mock
    private PostingErrorRepository errorRepository;

    @Mock
    private PostingDeadLetterRepository deadLetterRepository;

    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        monitoringService = new MonitoringService(
            batchExecutionRepository,
            errorRepository,
            deadLetterRepository
        );
    }

    // ============================================================
    // getBatchStatus() 테스트
    // ============================================================

    @Test
    void testGetBatchStatus_WithLatestExecution() {
        // Given: 배치 실행 이력이 있을 때
        BatchExecution batchExecution = new BatchExecution("SCHEDULED",
            LocalDate.of(2026, 6, 9), LocalDate.of(2026, 6, 15));

        when(batchExecutionRepository.findLatestExecution())
            .thenReturn(Optional.of(batchExecution));
        when(deadLetterRepository.count()).thenReturn(2L);
        when(errorRepository.count()).thenReturn(5L);

        // When: 배치 현황 조회 실행
        BatchStatusResponse response = monitoringService.getBatchStatus();

        // Then: 응답 검증
        assertNotNull(response);
        assertNotNull(response.lastExecution());
        assertEquals("SCHEDULED", response.lastExecution().batchType());
        assertEquals(2L, response.currentStats().totalDeadLetters());
        assertEquals(5L, response.currentStats().totalErrors());

        verify(batchExecutionRepository, times(1)).findLatestExecution();
        verify(deadLetterRepository, times(1)).count();
        verify(errorRepository, times(1)).count();
    }

    @Test
    void testGetBatchStatus_NoLatestExecution() {
        // Given: 배치 실행 이력이 없을 때
        when(batchExecutionRepository.findLatestExecution()).thenReturn(Optional.empty());
        when(deadLetterRepository.count()).thenReturn(0L);
        when(errorRepository.count()).thenReturn(0L);

        // When: 배치 현황 조회 실행
        BatchStatusResponse response = monitoringService.getBatchStatus();

        // Then: lastExecution이 null인지 검증
        assertNotNull(response);
        assertNull(response.lastExecution());
        assertEquals(0.0, response.currentStats().successRate());
    }

    // ============================================================
    // getErrors() 테스트
    // ============================================================

    @Test
    void testGetErrors_Success() {
        // Given: 에러가 1건 있을 때
        PostingError error = mock(PostingError.class);
        when(error.getId()).thenReturn(1L);
        when(error.getMemberId()).thenReturn(1L);
        when(error.getErrorMessage()).thenReturn("Test error");
        when(error.getRetryCount()).thenReturn(2);

        Pageable pageable = PageRequest.of(0, 10);
        Page<PostingError> errorPage = new PageImpl<>(List.of(error), pageable, 1);

        when(errorRepository.findAllByOrderByCreatedAtDesc(pageable))
            .thenReturn(errorPage);

        // When: 에러 로그 조회 실행
        Page<ErrorLogDto> result = monitoringService.getErrors(pageable);

        // Then: 에러 데이터 검증
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test error", result.getContent().get(0).errorMessage());
        assertEquals(2, result.getContent().get(0).retryCount());

        verify(errorRepository, times(1)).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void testGetErrors_Empty() {
        // Given: 에러가 없을 때
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostingError> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(errorRepository.findAllByOrderByCreatedAtDesc(pageable))
            .thenReturn(emptyPage);

        // When: 에러 로그 조회 실행
        Page<ErrorLogDto> result = monitoringService.getErrors(pageable);

        // Then: 빈 페이지 검증
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    // ============================================================
    // getDeadLetters() 테스트
    // ============================================================

    @Test
    void testGetDeadLetters_Success() {
        // Given: 데드레터가 1건 있을 때
        PostingDeadLetter deadLetter = mock(PostingDeadLetter.class);
        when(deadLetter.getId()).thenReturn(1L);
        when(deadLetter.getMemberId()).thenReturn(1L);
        when(deadLetter.getErrorMessage()).thenReturn("Dead letter test");

        Pageable pageable = PageRequest.of(0, 10);
        Page<PostingDeadLetter> deadLetterPage = new PageImpl<>(
            List.of(deadLetter), pageable, 1
        );

        when(deadLetterRepository.findAllByOrderByCreatedAtDesc(pageable))
            .thenReturn(deadLetterPage);

        // When: 데드레터 조회 실행
        Page<ErrorLogDto> result = monitoringService.getDeadLetters(pageable);

        // Then: 데드레터 데이터 검증
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Dead letter test", result.getContent().get(0).errorMessage());

        verify(deadLetterRepository, times(1)).findAllByOrderByCreatedAtDesc(pageable);
    }

    // ============================================================
    // retryError() 테스트
    // ============================================================

    @Test
    void testRetryError_Success() {
        // Given: 에러가 존재할 때
        PostingError error = mock(PostingError.class);
        when(error.getRetryCount()).thenReturn(0);

        when(errorRepository.findById(1L)).thenReturn(Optional.of(error));

        // When: 에러 재시도 실행
        Map<String, String> response = monitoringService.retryError(1L);

        // Then: 응답 검증 및 save 호출 확인
        assertEquals("QUEUED", response.get("status"));
        assertEquals("재시도 요청이 큐에 추가됨", response.get("message"));

        verify(errorRepository, times(1)).findById(1L);
        verify(errorRepository, times(1)).save(any(PostingError.class));
    }

    @Test
    void testRetryError_NotFound() {
        // Given: 에러가 없을 때
        when(errorRepository.findById(999L)).thenReturn(Optional.empty());

        // When: 에러 재시도 실행
        Map<String, String> response = monitoringService.retryError(999L);

        // Then: 응답은 동일하지만 save는 호출되지 않음
        assertEquals("QUEUED", response.get("status"));

        verify(errorRepository, times(1)).findById(999L);
        verify(errorRepository, times(0)).save(any(PostingError.class));
    }

    // ============================================================
    // retryDeadLetter() 테스트
    // ============================================================

    @Test
    void testRetryDeadLetter_Success() {
        // Given: 데드레터가 존재할 때
        PostingDeadLetter deadLetter = mock(PostingDeadLetter.class);

        when(deadLetterRepository.findById(1L)).thenReturn(Optional.of(deadLetter));

        // When: 데드레터 재시도 실행
        Map<String, String> response = monitoringService.retryDeadLetter(1L);

        // Then: 응답 검증 및 delete 호출 확인
        assertEquals("QUEUED", response.get("status"));

        verify(deadLetterRepository, times(1)).findById(1L);
        verify(deadLetterRepository, times(1)).delete(any(PostingDeadLetter.class));
    }

    @Test
    void testRetryDeadLetter_NotFound() {
        // Given: 데드레터가 없을 때
        when(deadLetterRepository.findById(999L)).thenReturn(Optional.empty());

        // When: 데드레터 재시도 실행
        Map<String, String> response = monitoringService.retryDeadLetter(999L);

        // Then: 응답은 동일하지만 delete는 호출되지 않음
        assertEquals("QUEUED", response.get("status"));

        verify(deadLetterRepository, times(1)).findById(999L);
        verify(deadLetterRepository, times(0)).delete(any(PostingDeadLetter.class));
    }
}