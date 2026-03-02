package com.jk.amazon2.exception;

public record ApiErrorResponse(
        String code,
        String message
){
    public static ApiErrorResponse of(ErrorCode errorCode) {
        return new ApiErrorResponse(errorCode.name(), errorCode.getMessage());
    }
    public static ApiErrorResponse of(String name, String message) {
        return new ApiErrorResponse(name, message);
    }
}
