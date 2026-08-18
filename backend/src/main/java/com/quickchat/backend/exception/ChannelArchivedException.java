package com.quickchat.backend.exception;

import org.springframework.http.HttpStatus;

/** ARCHIVED 채널에 메시지를 보내려 할 때. business-rules.md Q3 답변 B. */
public class ChannelArchivedException extends ApiException {
    public ChannelArchivedException() {
        super("CHANNEL_ARCHIVED", "보관된 채널에는 메시지를 보낼 수 없습니다.", HttpStatus.CONFLICT);
    }
}
