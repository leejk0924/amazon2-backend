package com.jk.amazon2.controller.spec;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.controller.dto.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "카테고리", description = "카테고리 관련 API")
public interface
CategoryApiSpec {
    @Operation(summary = "카테고리 단건 조회")
    ResponseEntity<CategoryResponse.Info> getCategory(String code);
    @Operation(summary = "카테고리 조회 및 검색")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<Page<CategoryResponse.Info>> getCategories(
            @ParameterObject CategoryRequest.CategorySearchCondition searchCondition,
            Pageable pageable
    );
    @Operation(summary = "카테고리 추가")
    @ApiResponse(responseCode = "201", description = "카테고리 추가 성공")
    ResponseEntity<CategoryResponse.CategoryCreateDto> createCategory(CategoryRequest.CategoryCreateDto categoryDto);
    @Operation(summary = "카테고리 수정")
    @ApiResponse(responseCode = "200", description = "카테고리 수정 성공")
    ResponseEntity<CategoryResponse.CategoryUpdateDto> updateCategory(String id, CategoryRequest.CategoryUpdateDto categoryUpdateDto);
    @Operation(summary = "카테고리 삭제")
    @ApiResponse(responseCode = "204", description = "카테고리 삭제 성공")
    ResponseEntity<Void> deleteCategory(Long memberId);
}
