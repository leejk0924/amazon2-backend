package com.jk.amazon2.member.dto;

import com.jk.amazon2.member.dto.MemberRequest;
import com.jk.amazon2.member.exception.MemberErrorCode;
import com.jk.amazon2.common.exception.RestApiException;
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
        private String name;
        private String categoryCode;

        public static Create of(String nickname, String name, String categoryCode) {
            return new Create(nickname, name, categoryCode);
        }

        public static Create from(MemberRequest.MemberDto dto) {
            return new Create(dto.nickname(), dto.name(), dto.categoryCode());
        }

        private Create(String nickname, String name, String categoryCode) {
            if (nickname == null || nickname.isBlank() || nickname.length() > 50) {
                log.warn("[VALIDATION_FAILED] MemberCommand.Create - Invalid nickname. nickname={}", nickname);
                throw new RestApiException(MemberErrorCode.MEMBER_NICKNAME_INVALID);
            }
            if (name != null && name.length() > 20) {
                log.warn("[VALIDATION_FAILED] MemberCommand.Create - Invalid name. name={}", name);
                throw new RestApiException(MemberErrorCode.MEMBER_NAME_INVALID);
            }
            this.nickname = nickname;
            this.name = name;
            this.categoryCode = categoryCode;
        }
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Update {

        private String currentNickname;
        private String nickname;
        private String name;
        private String categoryCode;

        public static Update of(String currentNickname, String nickname, String name, String categoryCode) {
            return new Update(currentNickname, nickname, name, categoryCode);
        }

        private Update(String currentNickname, String nickname, String name, String categoryCode) {
            if (currentNickname == null || currentNickname.isBlank()) {
                log.warn("[VALIDATION_FAILED] MemberCommand.Update - Invalid currentNickname. currentNickname={}", currentNickname);
                throw new RestApiException(MemberErrorCode.MEMBER_NICKNAME_INVALID);
            }
            if (nickname == null || nickname.isBlank() || nickname.length() > 50) {
                log.warn("[VALIDATION_FAILED] MemberCommand.Update - Invalid nickname. nickname={}", nickname);
                throw new RestApiException(MemberErrorCode.MEMBER_NICKNAME_INVALID);
            }
            if (name != null && name.length() > 20) {
                log.warn("[VALIDATION_FAILED] MemberCommand.Update - Invalid name. name={}", name);
                throw new RestApiException(MemberErrorCode.MEMBER_NAME_INVALID);
            }
            if (categoryCode != null && (categoryCode.isBlank() || categoryCode.length() > 10)) {
                log.warn("[VALIDATION_FAILED] MemberCommand.Update - Invalid categoryCode. categoryCode={}", categoryCode);
                throw new RestApiException(MemberErrorCode.MEMBER_CATEGORY_CODE_INVALID);
            }

            this.currentNickname = currentNickname;
            this.nickname = nickname;
            this.name = name;
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
