package com.jk.amazon2.controller;

import com.jk.amazon2.controller.dto.PostingRequest;
import com.jk.amazon2.controller.dto.PostingResponse;
import com.jk.amazon2.controller.spec.PostingApiSpec;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PostingController implements PostingApiSpec {

    @Override
    @GetMapping("/postings")
    public ResponseEntity<Page<PostingResponse.PostingDto>> getPostings(
            @ParameterObject PostingRequest.PostingSearchDto searchCondition,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        List<PostingResponse.PostingDto> content = List.of(
            new PostingResponse.PostingDto(1L, 3, 5, 2, 4, 6, 1, 0),
            new PostingResponse.PostingDto(2L, 1, 2, 3, 1, 4, 2, 1),
            new PostingResponse.PostingDto(3L, 0, 1, 2, 3, 5, 3, 2)
        );

        Page<PostingResponse.PostingDto> page = new PageImpl<>(
            content,
            pageable,
            content.size()
        ).map(m -> new PostingResponse.PostingDto(
                m.memberId(),
                m.mon(),
                m.tue(),
                m.wed(),
                m.thu(),
                m.fri(),
                m.sat(),
                m.sun()
        ));

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(page);
    }
}
