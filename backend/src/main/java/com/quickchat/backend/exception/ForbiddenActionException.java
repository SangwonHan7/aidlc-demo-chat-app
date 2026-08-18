package com.quickchat.backend.exception;

import org.springframework.http.HttpStatus;

/** OWNER 권한이 필요한 작업(초대/제외 등)을 OWNER가 아닌 사용자가 시도할 때. */
public class ForbiddenActionException extends ApiException {
    public ForbiddenActionException() {
        super("FORBIDDEN_ACTION", "이 작업을 수행할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
}
