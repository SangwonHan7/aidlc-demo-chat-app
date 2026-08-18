package com.quickchat.backend.domain;

/** GROUP 채널에만 의미 있음. DIRECT 채널은 항상 초대 전용 성격(값은 저장하되 정책상 참조하지 않음). */
public enum ChannelVisibility {
    PUBLIC,
    INVITE_ONLY
}
