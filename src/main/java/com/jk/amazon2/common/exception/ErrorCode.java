package com.jk.amazon2.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String name();
    HttpStatus status();
    String getMessage();
}
