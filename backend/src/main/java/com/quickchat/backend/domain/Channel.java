package com.quickchat.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Channel entity. type=DIRECT면 멤버 정확히 2명인 DM, type=GROUP이면 일반 채널.
 * 이름 중복 허용 (business-rules.md Q2 답변 A).
 */
@Entity
@Table(name = "channels")
public class Channel {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelVisibility visibility;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelStatus status = ChannelStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Channel() {
        // JPA
    }

    public Channel(String name, ChannelType type, ChannelVisibility visibility, UUID ownerId) {
        this.id = UUID.randomUUID(); // 클라이언트 측 ID 생성 - DB 라운드트립 없이 즉시 id 사용 가능 (테스트 용이성 포함)
        this.name = name;
        this.type = type;
        this.visibility = visibility;
        this.ownerId = ownerId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ChannelType getType() {
        return type;
    }

    public ChannelVisibility getVisibility() {
        return visibility;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public ChannelStatus getStatus() {
        return status;
    }

    public boolean isArchived() {
        return status == ChannelStatus.ARCHIVED;
    }

    public void archive() {
        this.status = ChannelStatus.ARCHIVED;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
