package com.jk.amazon2.service.dto;

import com.jk.amazon2.member.dto.MemberRequest;
import com.jk.amazon2.member.exception.MemberErrorCode;
import com.jk.amazon2.exception.RestApiException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberCommand {
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Create {
        private String nickname;
        private String categoryCode;

        public static Create of(String nickname, String categoryCode) {
            return new Create(nickname, categoryCode);
        }

        public static Create from(MemberRequest.MemberDto dto) {
            return new Create(dto.nickname(), dto.categoryCode());
        }

        private Create(String nickname, String categoryCode) {
            if (nickname == null || nickname.isBlank() || nickname.length() > 50) {
                log.warn("[VALIDATION_FAILED] MemberCommand.Detail - Invalid nickname. nickname={}", nickname);
                throw new RestApiException(MemberErrorCode.MEMBER_NICKNAME_INVALID);
            }
            this.nickname = nickname;
            this.categoryCode = categoryCode;
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Update {

        private Long id;
        private String nickname;
        private String categoryCode;

        public static Update of(Long id, String nickname, String categoryCode) {
            return new Update(id, nickname, categoryCode);
        }

        private Update(Long id, String nickname, String categoryCode) {
            if (nickname == null || nickname.isBlank() || nickname.length() > 50) {
                log.warn("[VALIDATION_FAILED] MemberCommand.Update - Invalid nickname. nickname={}", nickname);
                throw new RestApiException(MemberErrorCode.MEMBER_NICKNAME_INVALID);
            }

            if (categoryCode != null && (categoryCode.isBlank() || categoryCode.length() > 10)) {
                log.warn("[VALIDATION_FAILED] MemberCommand.Update - Invalid categoryCode. categoryCode={}", categoryCode);
                throw new RestApiException(MemberErrorCode.MEMBER_CATEGORY_CODE_INVALID);
            }

            this.id = id;
            this.nickname = nickname;
            this.categoryCode = categoryCode;
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class Search {
        private final String nickname;
        private final String categoryCode;
        private final Boolean deleted;

        public static Search from(MemberRequest.MemberSearchCondition condition) {
            Boolean deleted = switch (condition.status()) {
                case "active" -> false;
                case "deleted" -> true;
                case null,
                default -> null;
            };
            return new Search(condition.nickname(), condition.categoryCode(), deleted);
        }
    }
}
