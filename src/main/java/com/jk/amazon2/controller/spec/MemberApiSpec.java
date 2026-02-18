package com.jk.amazon2.controller.spec;

import com.jk.amazon2.controller.dto.MemberRequest;
import com.jk.amazon2.controller.dto.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "유저", description = "유저 관련 API")
public interface MemberApiSpec {
    @Operation(summary = "유저 조회 및 검색")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<Page<MemberResponse.MemberDto>> getMembers(
            @ParameterObject MemberRequest.MemberSearchCondition searchCondition,
            Pageable pageable
    );
    @Operation(summary = "유저 추가")
    @ApiResponse(responseCode = "201", description = "유저 추가 성공")
    ResponseEntity<MemberResponse.MemberDto> createMember(MemberRequest.MemberDto memberDto);
    @Operation(summary = "유저 수정")
    @ApiResponse(responseCode = "200", description = "유저 수정 성공")
    ResponseEntity<MemberResponse.MemberDto> updateMember(Long id, MemberRequest.MemberDto memberDto);
    @Operation(summary = "유저 삭제")
    @ApiResponse(responseCode = "204", description = "유저 삭제 성공")
    ResponseEntity<Void> deleteMember(Long memberId);
}
