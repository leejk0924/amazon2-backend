package com.jk.amazon2.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다"),
    CATEGORY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 카테고리 코드입니다"),
    CATEGORY_CODE_INVALID(HttpStatus.BAD_REQUEST, "카테고리 코드 형식이 올바르지 않습니다"),
    CATEGORY_NAME_EMPTY(HttpStatus.BAD_REQUEST, "카테고리 이름은 필수입니다"),
    CATEGORY_DESCRIPTION_INVALID(HttpStatus.BAD_REQUEST, "카테고리 설명은 최대 50자까지 입력 가능합니다");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus status() {
        return this.httpStatus;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
