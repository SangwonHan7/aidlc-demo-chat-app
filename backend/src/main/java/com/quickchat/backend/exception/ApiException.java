package com.quickchat.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * 공통 에러 포맷의 기반 예외. tech-env.md: { "errorCode": "...", "message": "..." }
 */
public class ApiException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus status;

    public ApiException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
