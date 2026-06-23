package com.jk.amazon2.member.controller.spec;

import com.jk.amazon2.member.dto.MemberRequest;
import com.jk.amazon2.member.dto.MemberResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@Tag(name = "유저", description = "유저 관련 API")
public interface MemberApiSpec {
    @Operation(summary = "유저 단건 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<MemberResponse.MemberDetailDto> getMember(String nickname);
    @Operation(summary = "유저 조회 및 검색")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<Page<MemberResponse.MemberListDto>> getMembers(
            @ParameterObject MemberRequest.MemberSearchCondition searchCondition,
            Pageable pageable
    );
    @Operation(summary = "유저 추가")
    @ApiResponse(responseCode = "201", description = "유저 추가 성공")
    ResponseEntity<MemberResponse.MemberCreateDto> createMember(MemberRequest.MemberCreateDto memberDto);
    @Operation(summary = "유저 수정")
    @ApiResponse(responseCode = "200", description = "유저 수정 성공")
    ResponseEntity<MemberResponse.MemberUpdateDto> updateMember(String nickname, MemberRequest.MemberDto memberDto);
    @Operation(summary = "유저 삭제")
    @ApiResponse(responseCode = "204", description = "유저 삭제 성공")
    ResponseEntity<Void> deleteMember(String nickname);
    @Operation(summary = "유저 영구 삭제")
    @ApiResponse(responseCode = "204", description = "유저 영구 삭제 성공")
    ResponseEntity<Void> hardDeleteMember(String nickname);

    @Operation(summary = "soft delete된 유저 복구")
    @ApiResponse(responseCode = "204", description = "유저 복구 성공")
    @ApiResponse(responseCode = "400", description = "이미 활성 상태인 회원")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 회원")
    ResponseEntity<Void> restoreMember(String nickname);
}
