package com.jk.amazon2.posting.dto;

import com.jk.amazon2.posting.entity.Posting;

import java.time.LocalDate;

public class PostingResponse {
    public record PostingDto(
            Long memberId,
            String memberNickname,
            LocalDate weekStartDate,
            int mon,
            int tue,
            int wed,
            int thu,
            int fri,
            int sat,
            int sun
    ) {
        public static PostingDto from(Posting posting, String memberNickname) {
            return new PostingDto(
                posting.getMemberId(),
                memberNickname,
                posting.getWeekStartDate(),
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
