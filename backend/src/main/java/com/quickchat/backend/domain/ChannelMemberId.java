package com.quickchat.backend.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** ChannelMember의 복합키 클래스 (JPA @IdClass 요구사항). */
public class ChannelMemberId implements Serializable {

    private UUID channelId;
    private UUID userId;

    public ChannelMemberId() {
    }

    public ChannelMemberId(UUID channelId, UUID userId) {
        this.channelId = channelId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChannelMemberId that)) return false;
        return Objects.equals(channelId, that.channelId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, userId);
    }
}
