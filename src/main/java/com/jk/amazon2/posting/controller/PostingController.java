package com.jk.amazon2.posting.controller;

import com.jk.amazon2.posting.dto.BatchRequest;
import com.jk.amazon2.posting.dto.PostingRequest;
import com.jk.amazon2.posting.dto.PostingResponse;
import com.jk.amazon2.posting.service.BatchService;
import com.jk.amazon2.posting.service.PostingService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PostingController implements PostingApiSpec {

    private final PostingService postingService;
    private final BatchService batchService;

    @Override
    @GetMapping("/postings")
    public ResponseEntity<Page<PostingResponse.PostingDto>> getPostings(
            @ParameterObject PostingRequest.PostingSearchDto searchCondition,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<PostingResponse.PostingDto> page = postingService.getPostings(
            searchCondition.startDate(), pageable
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(page);
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> executeBatch(
        @RequestBody BatchRequest request
    ) {
        Long batchId = batchService.executeBatch(
            request.startDate(),
            request.endDate(),
            "MANUAL"
        );

        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(Map.of(
                "batchExecutionId", batchId,
                "status", "IN_PROGRESS",
                "message", "배치 작업 시작됨"
            ));
    }
}
