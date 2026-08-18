package com.quickchat.backend.exception;

import org.springframework.http.HttpStatus;

/** 사용자당 메시지 전송 rate limit 초과. nfr-design-patterns.md Q4 답변 A. */
public class RateLimitedException extends ApiException {
    public RateLimitedException() {
        super("RATE_LIMITED", "메시지 전송 속도가 너무 빠릅니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS);
    }
}
