package com.jk.amazon2.controller;

import com.jk.amazon2.controller.dto.MemberRequest;
import com.jk.amazon2.controller.dto.MemberResponse;
import com.jk.amazon2.controller.spec.MemberApiSpec;
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
    public ResponseEntity<MemberResponse.MemberDto> createMember(
            @RequestBody MemberRequest.MemberDto memberDto
    ) {
        MemberResponse.MemberDto savedMember = new MemberResponse.MemberDto(
                memberDto.nickname(),
                memberDto.categoryCode(),
                LocalDate.now(),
                "active"
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedMember);
    }

    @Override
    @PutMapping("/members/{id}")
    public ResponseEntity<MemberResponse.MemberDto> updateMember(
            @PathVariable Long id,
            @RequestBody MemberRequest.MemberDto member
    ) {
        MemberResponse.MemberDto updatedMember = new MemberResponse.MemberDto(member.nickname(), member.categoryCode(), LocalDate.now(), "active");
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updatedMember);
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
