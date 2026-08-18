package com.quickchat.backend.exception;

import org.springframework.http.HttpStatus;

/** 로그인 5회 연속 실패로 잠긴 계정. business-rules.md Auth 섹션. */
public class AccountLockedException extends ApiException {
    public AccountLockedException() {
        super("ACCOUNT_LOCKED", "로그인 실패 횟수가 초과되어 계정이 잠겼습니다. 잠시 후 다시 시도해주세요.", HttpStatus.LOCKED);
    }
}
