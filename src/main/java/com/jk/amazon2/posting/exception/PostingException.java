package com.jk.amazon2.posting.exception;

public class PostingException extends RuntimeException {
    public PostingException(String message) {
        super(message);
    }

    public PostingException(String message, Throwable cause) {
        super(message, cause);
    }
}
