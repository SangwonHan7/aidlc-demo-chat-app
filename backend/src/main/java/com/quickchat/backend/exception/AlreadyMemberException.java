package com.quickchat.backend.exception;

import org.springframework.http.HttpStatus;

/** 이미 멤버인 사용자를 재초대/재참여시킬 때. business-rules.md Q4 답변 A (멱등 처리 아님). */
public class AlreadyMemberException extends ApiException {
    public AlreadyMemberException() {
        super("ALREADY_MEMBER", "이미 채널의 멤버입니다.", HttpStatus.CONFLICT);
    }
}
