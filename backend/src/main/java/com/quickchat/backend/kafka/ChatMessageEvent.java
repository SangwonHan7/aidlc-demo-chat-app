package com.quickchat.backend.kafka;

import java.time.Instant;
import java.util.UUID;

/** chat-messages 토픽으로 발행/구독되는 이벤트 payload (JSON 직렬화). */
public record ChatMessageEvent(
        UUID messageId,
        UUID channelId,
        UUID senderId,
        String content,
        Instant sentAt
) {
}
