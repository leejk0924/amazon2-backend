package com.jk.amazon2.posting.dto;

public class PostingResponse {
    public record PostingDto(
            Long memberId,
            int mon,
            int tue,
            int wed,
            int thu,
            int fri,
            int sat,
            int sun
    ) {}
}
