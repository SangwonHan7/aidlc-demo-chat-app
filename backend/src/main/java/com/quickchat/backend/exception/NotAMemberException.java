package com.quickchat.backend.exception;

import org.springframework.http.HttpStatus;

/** 채널 멤버가 아닌 사용자가 채널에 접근(메시지 전송/조회)하려 할 때. */
public class NotAMemberException extends ApiException {
    public NotAMemberException() {
        super("NOT_A_MEMBER", "채널의 멤버가 아닙니다.", HttpStatus.FORBIDDEN);
    }
}
