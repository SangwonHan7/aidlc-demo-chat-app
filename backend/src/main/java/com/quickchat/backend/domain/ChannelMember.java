package com.quickchat.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** 복합키(channelId, userId). DIRECT 채널은 정확히 2건이 존재해야 한다 (domain-entities.md). */
@Entity
@Table(name = "channel_members")
@IdClass(ChannelMemberId.class)
public class ChannelMember {

    @Id
    @Column(name = "channel_id")
    private UUID channelId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    protected ChannelMember() {
        // JPA
    }

    public ChannelMember(UUID channelId, UUID userId, ChannelRole role) {
        this.channelId = channelId;
        this.userId = userId;
        this.role = role;
    }

    public UUID getChannelId() {
        return channelId;
    }

    public UUID getUserId() {
        return userId;
    }

    public ChannelRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
