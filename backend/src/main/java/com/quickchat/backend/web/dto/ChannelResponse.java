package com.quickchat.backend.web.dto;

import com.quickchat.backend.domain.Channel;
import com.quickchat.backend.domain.ChannelStatus;
import com.quickchat.backend.domain.ChannelType;
import com.quickchat.backend.domain.ChannelVisibility;

import java.time.Instant;
import java.util.UUID;

public record ChannelResponse(
        UUID id,
        String name,
        ChannelType type,
        ChannelVisibility visibility,
        UUID ownerId,
        ChannelStatus status,
        Instant createdAt
) {
    public static ChannelResponse from(Channel channel) {
        return new ChannelResponse(channel.getId(), channel.getName(), channel.getType(),
                channel.getVisibility(), channel.getOwnerId(), channel.getStatus(), channel.getCreatedAt());
    }
}
