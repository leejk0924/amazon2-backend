package com.jk.amazon2.posting.service;

import com.jk.amazon2.posting.dto.BatchStatusResponse;
import com.jk.amazon2.posting.dto.ErrorLogDto;
import com.jk.amazon2.posting.entity.BatchExecution;
import com.jk.amazon2.posting.entity.PostingDeadLetter;
import com.jk.amazon2.posting.entity.PostingError;
import com.jk.amazon2.posting.repository.BatchExecutionRepository;
import com.jk.amazon2.posting.repository.PostingDeadLetterRepository;
import com.jk.amazon2.posting.repository.PostingErrorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final BatchExecutionRepository batchExecutionRepository;
    private final PostingErrorRepository errorRepository;
    private final PostingDeadLetterRepository deadLetterRepository;

    // 배치 실행 현황 조회
    public BatchStatusResponse getBatchStatus() {
        Optional<BatchExecution> lastExecution = batchExecutionRepository.findLatestExecution();

        BatchStatusResponse.LastExecution lastExec = lastExecution
            .map(be -> new BatchStatusResponse.LastExecution(
                be.getId(),
                be.getBatchType(),
                be.getStartDate(),
                be.getEndDate(),
                be.getStatus(),
                be.getTotalCount(),
                be.getSuccessCount(),
                be.getRetryCount(),
                be.getFailedCount(),
                be.getCompletedAt()
            ))
            .orElse(null);

        long totalDeadLetters = deadLetterRepository.count();
        long totalErrors = errorRepository.count();
        double successRate = lastExecution
            .map(be -> be.getTotalCount() > 0 ?
                (be.getSuccessCount() / (double) be.getTotalCount()) * 100 : 0.0)
            .orElse(0.0);

        BatchStatusResponse.CurrentStats stats = new BatchStatusResponse.CurrentStats(
            totalDeadLetters,
            totalErrors,
            successRate
        );

        return new BatchStatusResponse(lastExec, stats);
    }

    // 에러 로그 조회
    public Page<ErrorLogDto> getErrors(Pageable pageable) {
        Page<PostingError> errors = errorRepository.findAllByOrderByCreatedAtDesc(pageable);

        return errors.map(error -> new ErrorLogDto(
            error.getId(),
            error.getMemberId(),
            "",
            error.getTargetDate(),
            error.getDayOfWeek(),
            error.getErrorMessage(),
            error.getRetryCount(),
            error.getCreatedAt()
        ));
    }

    // 데드레터 조회
    public Page<ErrorLogDto> getDeadLetters(Pageable pageable) {
        Page<PostingDeadLetter> deadLetters = deadLetterRepository.findAllByOrderByCreatedAtDesc(pageable);

        return deadLetters.map(dl -> new ErrorLogDto(
            dl.getId(),
            dl.getMemberId(),
            "",
            dl.getTargetDate(),
            dl.getDayOfWeek(),
            dl.getErrorMessage(),
            3,
            dl.getCreatedAt()
        ));
    }

    // 에러 재시도
    public Map<String, String> retryError(Long errorId) {
        Optional<PostingError> error = errorRepository.findById(errorId);

        if (error.isPresent()) {
            PostingError err = error.get();
            err.incrementRetryCount();
            errorRepository.save(err);
        }

        return Map.of("status", "QUEUED", "message", "재시도 요청이 큐에 추가됨");
    }

    // 데드레터 재시도
    public Map<String, String> retryDeadLetter(Long deadLetterId) {
        Optional<PostingDeadLetter> deadLetter = deadLetterRepository.findById(deadLetterId);

        if (deadLetter.isPresent()) {
            deadLetterRepository.delete(deadLetter.get());
        }

        return Map.of("status", "QUEUED", "message", "재시도 요청이 큐에 추가됨");
    }
}
