package com.jk.amazon2.posting.exception;

public class ParsingException extends PostingException {
    public ParsingException(String message) {
        super(message);
    }

    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
