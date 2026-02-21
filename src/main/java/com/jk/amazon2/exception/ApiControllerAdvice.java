package com.jk.amazon2.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiControllerAdvice {
    @ExceptionHandler(value = RestApiException.class)
    public ResponseEntity<ApiErrorResponse> handleRestApiException(RestApiException ex) {
        return ResponseEntity
                .status(ex.getErrorCode().status())
                .body(ApiErrorResponse.of(ex.getErrorCode()));
    }
}
