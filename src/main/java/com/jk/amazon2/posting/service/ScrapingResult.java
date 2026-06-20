package com.jk.amazon2.posting.service;

public sealed interface ScrapingResult<T> permits ScrapingResult.Success, ScrapingResult.Failure {

    record Success<T>(T value) implements ScrapingResult<T> {}

    record Failure<T>(FailureType type, String message, Throwable cause) implements ScrapingResult<T> {}

    enum FailureType {
        NETWORK_ERROR,  // IOException, InterruptedException → 재시도 가능
        HTTP_ERROR,     // HTTP status != 200 → 재시도 가능
        PARSING_ERROR   // HTML 요소 없음, 구조 변경 → 재시도 불필요
    }
}
