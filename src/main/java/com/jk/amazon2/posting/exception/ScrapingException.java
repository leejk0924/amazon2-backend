package com.jk.amazon2.posting.exception;

public class ScrapingException extends PostingException {
    public ScrapingException(String message) {
        super(message);
    }

    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
}
