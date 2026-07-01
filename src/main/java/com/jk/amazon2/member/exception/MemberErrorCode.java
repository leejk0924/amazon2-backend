package com.jk.amazon2.member.exception;

import com.jk.amazon2.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode{
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    MEMBER_NOT_DELETED(HttpStatus.BAD_REQUEST, "소프트 삭제된 회원만 영구 삭제가 가능합니다."),
    MEMBER_ALREADY_ACTIVE(HttpStatus.BAD_REQUEST, "이미 활성 상태인 회원입니다."),
    MEMBER_NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "닉네임은 필수 입니다."),
    MEMBER_NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 닉네임 입니다."),
    MEMBER_NICKNAME_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 닉네임 입니다."),
    MEMBER_CATEGORY_CODE_INVALID(HttpStatus.BAD_REQUEST, "카테고리 코드는 최대 10자까지 입력 가능합니다."),
    MEMBER_NAME_INVALID(HttpStatus.BAD_REQUEST, "이름은 최대 50자까지 입력 가능합니다.");

    private final HttpStatus httpStatus;
    private final String message;
    @Override
    public HttpStatus status() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
