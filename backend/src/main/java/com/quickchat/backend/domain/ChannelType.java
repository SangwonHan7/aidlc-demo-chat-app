package com.quickchat.backend.domain;

/** DIRECT = 1:1 DM (정확히 2명), GROUP = 그룹 채널. business-rules.md 참고. */
public enum ChannelType {
    DIRECT,
    GROUP
}
