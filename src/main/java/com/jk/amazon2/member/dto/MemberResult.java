package com.jk.amazon2.member.dto;

import com.jk.amazon2.member.entity.Member;
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
        private String name;
        private String categoryCode;
        private LocalDateTime createdAt;
        private boolean deleted;

        public static Detail from(Member member) {
            return new Detail(
                    member.getId(),
                    member.getNickname(),
                    member.getName(),
                    member.getCategoryCode(),
                    member.getCreatedAt(),
                    member.isDeleted()
            );
        }
    }

    public record Update(
            String nickname,
            String name,
            String categoryCode
    ) {
        public static Update of(String nickname, String name, String categoryCode) {
            return new Update(nickname, name, categoryCode);
        }
    }

    public record Summary(
            String nickname,
            String name,
            String categoryName,
            LocalDateTime createdAt,
            boolean deleted
    ) {
    }
}
