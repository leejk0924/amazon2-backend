package com.jk.amazon2.posting.dto;

import com.jk.amazon2.member.entity.Member;
import com.jk.amazon2.posting.entity.Posting;

import java.time.LocalDate;

public class PostingResponse {
    public record PostingDto(
            Long memberId,
            String memberNickname,
            String memberName,
            LocalDate weekStartDate,
            int mon,
            int tue,
            int wed,
            int thu,
            int fri,
            int sat,
            int sun
    ) {
        public static PostingDto from(Member member, Posting posting, LocalDate weekStartDate) {
            return new PostingDto(
                member.getId(),
                member.getNickname(),
                member.getName(),
                posting != null ? posting.getWeekStartDate() : weekStartDate,
                posting != null ? posting.getMon() : 0,
                posting != null ? posting.getTue() : 0,
                posting != null ? posting.getWed() : 0,
                posting != null ? posting.getThu() : 0,
                posting != null ? posting.getFri() : 0,
                posting != null ? posting.getSat() : 0,
                posting != null ? posting.getSun() : 0
            );
        }
    }
}
