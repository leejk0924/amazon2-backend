package com.jk.amazon2.posting.dto;

import com.jk.amazon2.posting.entity.Posting;

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
    ) {
        public static PostingDto from(Posting posting) {
            return new PostingDto(
                posting.getMemberId(),
                posting.getMon(),
                posting.getTue(),
                posting.getWed(),
                posting.getThu(),
                posting.getFri(),
                posting.getSat(),
                posting.getSun()
            );
        }
    }
}
