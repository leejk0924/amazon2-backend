package com.jk.amazon2.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid");
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
