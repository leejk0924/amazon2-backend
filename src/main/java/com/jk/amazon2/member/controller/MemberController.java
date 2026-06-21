package com.jk.amazon2.member.controller;

import com.jk.amazon2.member.dto.MemberRequest;
import com.jk.amazon2.member.dto.MemberResponse;
import com.jk.amazon2.member.controller.spec.MemberApiSpec;
import com.jk.amazon2.member.service.MemberService;
import com.jk.amazon2.member.dto.MemberCommand;
import com.jk.amazon2.member.dto.MemberResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberApiSpec {
    private final MemberService memberService;

    @Override
    @GetMapping("/members/{id}")
    public ResponseEntity<MemberResponse.MemberDetailDto> getMember(@PathVariable Long id) {
        MemberResult.Detail result = memberService.findById(id);
        return ResponseEntity.ok(MemberResponse.MemberDetailDto.from(result));
    }

    @Override
    @GetMapping("/members")
    public ResponseEntity<Page<MemberResponse.MemberListDto>> getMembers(
            MemberRequest.MemberSearchCondition searchCondition,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MemberResult.Summary> results = memberService.findMembers(searchCondition, pageable);
        Page<MemberResponse.MemberListDto> response = results.map(MemberResponse.MemberListDto::from);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Override
    @PostMapping("/members")
    public ResponseEntity<MemberResponse.MemberCreateDto> createMember(
            @RequestBody @Valid MemberRequest.MemberCreateDto request
    ) {
        MemberCommand.Create command = MemberCommand.Create.of(request.nickname(), request.name(), request.categoryCode());
        var createdMember = memberService.create(command);

        var response = MemberResponse.MemberCreateDto.from(createdMember);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Override
    @PutMapping("/members/{id}")
    public ResponseEntity<MemberResponse.MemberUpdateDto> updateMember(
            @PathVariable Long id,
            @RequestBody MemberRequest.MemberDto member
    ) {
        var update = MemberCommand.Update.of(id, member.nickname(), member.name(), member.categoryCode());

        var response = MemberResponse.MemberUpdateDto.from(memberService.update(update));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @Override
    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable Long id
    ) {
        memberService.delete(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }

    @Override
    @DeleteMapping("/members/{id}/permanent")
    public ResponseEntity<Void> hardDeleteMember(
            @PathVariable Long id
    ) {
        memberService.hardDelete(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
