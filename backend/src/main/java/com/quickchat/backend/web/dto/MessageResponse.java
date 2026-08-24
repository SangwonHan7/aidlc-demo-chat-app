package com.quickchat.backend.web.dto;

import com.quickchat.backend.domain.Message;
import com.quickchat.backend.kafka.ChatMessageEvent;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, UUID channelId, UUID senderId, String content, Instant sentAt) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(message.getId(), message.getChannelId(), message.getSenderId(),
                message.getContent(), message.getSentAt());
    }

    /**
     * Build and Test 단계의 정적 계약 감사에서 발견: WebSocket 실시간 브로드캐스트가 이 팩토리를 거치지 않고
     * {@code ChatMessageEvent}(내부 Kafka/Redis 전송용, 필드명 {@code messageId})를 그대로 STOMP로
     * 내보내고 있어, Frontend가 기대하는 {@code id} 필드가 없는 문제가 있었다. REST 이력 조회
     * (`MessageResponse.from(Message)`)와 동일한 클라이언트 노출 형태로 맞추기 위해 추가 - 내부 이벤트
     * 스키마(Kafka)와 클라이언트에게 보이는 응답 형태(REST/WS 공통)를 분리하는 목적.
     */
    public static MessageResponse from(ChatMessageEvent event) {
        return new MessageResponse(event.messageId(), event.channelId(), event.senderId(),
                event.content(), event.sentAt());
    }
}
