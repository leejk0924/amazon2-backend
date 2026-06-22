package com.jk.amazon2.posting.exception;

import com.jk.amazon2.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostingErrorCode implements ErrorCode {
    POSTING_NOT_FOUND(HttpStatus.NOT_FOUND, "포스팅을 찾을 수 없습니다"),
    POSTING_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 포스팅입니다"),
    POSTING_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "포스팅 저장에 실패했습니다"),
    INVALID_WEEK_START_DATE(HttpStatus.BAD_REQUEST, "주간 시작일은 월요일이어야 합니다");

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
