package com.quickchat.backend.domain;

/** ARCHIVED 채널은 신규 메시지 저장을 거부한다. business-rules.md Q3 답변 B. */
public enum ChannelStatus {
    ACTIVE,
    ARCHIVED
}
