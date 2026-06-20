package com.jk.amazon2.posting.controller;

import com.jk.amazon2.posting.dto.BatchStatusResponse;
import com.jk.amazon2.posting.dto.ErrorLogDto;
import com.jk.amazon2.posting.dto.StatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;

@Tag(name = "모니터링", description = "배치 모니터링 및 통계 API")
public interface MonitoringApiSpec {

    @Operation(summary = "배치 실행 상태 조회", description = "가장 최근 배치 실행 결과를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<BatchStatusResponse> getBatchStatus();

    @Operation(summary = "스크래핑 에러 목록 조회", description = "재시도 가능한 에러 목록을 페이징 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<Page<ErrorLogDto>> getErrors(Pageable pageable);

    @Operation(summary = "데드레터 목록 조회", description = "3회 재시도 후 실패한 항목 목록을 페이징 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<Page<ErrorLogDto>> getDeadLetters(Pageable pageable);

    @Operation(summary = "에러 재시도", description = "특정 에러 항목을 즉시 재시도합니다.")
    @ApiResponse(responseCode = "200", description = "재시도 요청 성공")
    @ApiResponse(responseCode = "404", description = "에러 항목 없음")
    ResponseEntity<Map<String, String>> retryError(
            @Parameter(description = "에러 ID") Long errorId
    );

    @Operation(summary = "데드레터 재시도", description = "특정 데드레터 항목을 즉시 재시도합니다.")
    @ApiResponse(responseCode = "200", description = "재시도 요청 성공")
    @ApiResponse(responseCode = "404", description = "데드레터 항목 없음")
    ResponseEntity<Map<String, String>> retryDeadLetter(
            @Parameter(description = "데드레터 ID") Long deadLetterId
    );

    @Operation(summary = "포스팅 통계 조회", description = "기간별 멤버별 포스팅 수를 집계합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<StatisticsResponse> getStatistics(
            @Parameter(description = "시작일 (yyyy-MM-dd)") LocalDate startDate,
            @Parameter(description = "종료일 (yyyy-MM-dd)") LocalDate endDate
    );
}
