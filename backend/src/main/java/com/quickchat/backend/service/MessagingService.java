package com.quickchat.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickchat.backend.domain.Message;
import com.quickchat.backend.exception.ChannelArchivedException;
import com.quickchat.backend.kafka.ChatMessageEvent;
import com.quickchat.backend.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * MessagingComponent 구현. business-logic-model.md 참고.
 */
@Service
public class MessagingService {

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class);
    private static final int DEFAULT_PAGE_SIZE = 50;
    public static final String BROADCAST_CHANNEL_PREFIX = "ws-broadcast:";

    private final MessageRepository messageRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public MessagingService(MessageRepository messageRepository, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** 저장 전 채널이 ARCHIVED면 거부한다 (business-rules.md). 호출자(ChatFacadeService)가 멤버십을 먼저 확인한다. */
    public Message saveMessage(UUID channelId, UUID senderId, String content, boolean channelArchived) {
        if (channelArchived) {
            throw new ChannelArchivedException();
        }
        Message message = new Message(channelId, senderId, content);
        return messageRepository.save(message);
    }

    /**
     * Cursor 기반 페이지네이션, 최신순. business-rules.md Q5 답변 A.
     * beforeSentAt이 null이면 최신 페이지, 아니면 그 시각 이전 페이지를 조회한다 - PostgreSQL의 null
     * 파라미터 타입 추론 문제(MessageRepository.java 참고)를 피하기 위해 두 개의 별도 쿼리로 분기한다.
     */
    public List<Message> getMessageHistory(UUID channelId, Instant beforeSentAt, int pageSize) {
        int size = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
        PageRequest page = PageRequest.of(0, size);
        return beforeSentAt == null
                ? messageRepository.findByChannelIdOrderBySentAtDesc(channelId, page)
                : messageRepository.findByChannelIdAndSentAtLessThanOrderBySentAtDesc(channelId, beforeSentAt, page);
    }

    /**
     * Redis Pub/Sub로 파드 간 브로드캐스트한다. 실제 WebSocket 세션 전달은
     * websocket.RedisBroadcastListener가 이 채널을 구독해 처리한다
     * (component-dependency.md의 메시지 전송 시퀀스 5-6단계).
     */
    public void broadcastMessage(ChatMessageEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            redis.convertAndSend(BROADCAST_CHANNEL_PREFIX + event.channelId(), payload);
        } catch (JsonProcessingException e) {
            // 직렬화 실패는 브로드캐스트만 실패시키고 메시지 저장 자체는 이미 완료된 상태이므로 예외를 전파하지 않는다.
            log.error("Failed to serialize broadcast event for channel {}", event.channelId(), e);
        }
    }
}
