package com.quickchat.backend.exception;

import org.springframework.http.HttpStatus;

public class ChannelNotFoundException extends ApiException {
    public ChannelNotFoundException() {
        super("CHANNEL_NOT_FOUND", "채널을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
    }
}
