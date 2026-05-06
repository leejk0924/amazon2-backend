package com.jk.amazon2.controller;

import com.jk.amazon2.controller.dto.MemberRequest;
import com.jk.amazon2.controller.dto.MemberResponse;
import com.jk.amazon2.controller.spec.MemberApiSpec;
import com.jk.amazon2.service.MemberService;
import com.jk.amazon2.service.dto.MemberCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController implements MemberApiSpec {
    private final MemberService memberService;

    @Override
    @GetMapping("/members")
    public ResponseEntity<Page<MemberResponse.MemberDto>> getMembers(
            MemberRequest.MemberSearchCondition searchCondition,
            @PageableDefault(size = 10, sort = "nickname") Pageable pageable
    ) {
        List<MemberResponse.MemberDto> content = List.of(
                new MemberResponse.MemberDto("user1", "카테고리1", LocalDate.of(2026, 2, 10), "active"),
                new MemberResponse.MemberDto("user2", "카테고리2", LocalDate.of(2026, 2, 11), "active")
        );

        Page<MemberResponse.MemberDto> data = new PageImpl<>(
            content,
            pageable,
            content.size()
        ).map(m -> new MemberResponse.MemberDto(
                m.nickname(),
                m.categoryName(),
                m.joinDate(),
                m.status()
        ));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(data);
    }

    @Override
    @PostMapping("/members")
    public ResponseEntity<MemberResponse.MemberCreateDto> createMember(
            @RequestBody @Valid MemberRequest.MemberCreateDto request
    ) {
        MemberCommand.Create command = MemberCommand.Create.of(request.nickname(), request.categoryCode());
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
        var update = MemberCommand.Update.of(member.nickname(), member.categoryCode());

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
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
