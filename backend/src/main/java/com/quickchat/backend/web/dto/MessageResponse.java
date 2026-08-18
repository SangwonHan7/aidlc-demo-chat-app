package com.quickchat.backend.web.dto;

import com.quickchat.backend.domain.Message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, UUID channelId, UUID senderId, String content, Instant sentAt) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getChannelId(), message.getSenderId(),
                message.getContent(), message.getSentAt());
    }
}
