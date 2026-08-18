package com.quickchat.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickchat.backend.kafka.ChatMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub("ws-broadcast:*")를 구독해 이 파드에 연결된 WebSocket 세션으로 전달한다.
 * component-dependency.md 메시지 전송 시퀀스 6단계: "MessagingComponent -> Redis Pub/Sub ->
 * 각 파드의 WebSocket 세션 -> 클라이언트 수신"의 실제 구현체.
 */
@Component
public class RedisBroadcastListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(RedisBroadcastListener.class);
    private static final String CHANNEL_PREFIX = "ws-broadcast:";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public RedisBroadcastListener(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channelKey = new String(message.getChannel(), StandardCharsets.UTF_8);
        String channelId = channelKey.substring(CHANNEL_PREFIX.length());
        try {
            ChatMessageEvent event = objectMapper.readValue(message.getBody(), ChatMessageEvent.class);
            messagingTemplate.convertAndSend("/topic/channel/" + channelId, event);
        } catch (Exception e) {
            log.error("Failed to relay broadcast message for channel {}", channelId, e);
        }
    }
}
