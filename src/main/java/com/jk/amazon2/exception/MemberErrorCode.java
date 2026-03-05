package com.jk.amazon2.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode{
    MEMBER_NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "닉네임은 필수 입니다."),
    MEMBER_NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 닉네임 입니다.");

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
