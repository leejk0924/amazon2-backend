package com.jk.amazon2.controller.spec;

import com.jk.amazon2.controller.dto.PostingRequest;
import com.jk.amazon2.controller.dto.PostingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "포스팅", description = "포스팅 관련 API")
public interface PostingApiSpec {
    @Operation(summary = "포스팅 조회 및 검색")
    @ApiResponse(responseCode = "200", description = "포스팅 조회 성공")
    ResponseEntity<Page<PostingResponse.PostingDto>> getPostings(
            @ParameterObject PostingRequest.PostingSearchDto searchCondition,
            Pageable pageable
    );
}
