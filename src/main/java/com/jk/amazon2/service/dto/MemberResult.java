package com.jk.amazon2.service.dto;

import com.jk.amazon2.entity.Member;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberResult {

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PUBLIC, staticName = "of")
    public static class Detail {
        private Long id;
        private String nickname;
        private String categoryCode;
        private LocalDateTime createdAt;

        public static Detail from(Member member) {
            return new Detail(
                    member.getId(),
                    member.getNickname(),
                    member.getCategoryCode(),
                    member.getCreatedAt()
            );
        }
    }

    public record Update(
            String nickname,
            String categoryCode
    ) {
        public static Update of(String nickname, String categoryCode) {
            return new Update(nickname, categoryCode);
        }
    }
}