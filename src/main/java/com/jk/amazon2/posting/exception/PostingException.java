package com.jk.amazon2.posting.exception;

import com.jk.amazon2.common.exception.RestApiException;

public class PostingException extends RestApiException {
    public PostingException(PostingErrorCode errorCode) {
        super(errorCode);
    }
}
