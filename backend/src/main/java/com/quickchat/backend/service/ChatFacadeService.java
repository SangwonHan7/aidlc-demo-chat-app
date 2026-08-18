package com.quickchat.backend.service;

import com.quickchat.backend.domain.Channel;
import com.quickchat.backend.domain.Message;
import com.quickchat.backend.exception.RateLimitedException;
import com.quickchat.backend.kafka.ChatMessageEvent;
import com.quickchat.backend.kafka.EventPublisher;
import com.quickchat.backend.kafka.KafkaTopics;
import com.quickchat.backend.redis.MessageRateLimitService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * ChatFacadeService (Orchestrator). 컴포넌트 2개 이상을 가로지르는 흐름을 조정한다
 * (services.md, component-dependency.md의 메시지 전송 시퀀스).
 *
 * sendMessage 시퀀스:
 * 1) rate limit 확인 -> 2) 멤버십 확인 -> 3) 메시지 저장 -> 4) Kafka 발행
 * (실제 WebSocket 브로드캐스트는 EventComponent의 Kafka 컨슈머 측(ChatMessageConsumer)에서
 * MessagingService.broadcastMessage()를 트리거하여 처리한다 - 저장과 브로드캐스트를 분리해
 * Kafka를 통한 재처리가 가능하게 한다).
 */
@Service
public class ChatFacadeService {

    private final ChannelService channelService;
    private final MessagingService messagingService;
    private final EventPublisher eventPublisher;
    private final MessageRateLimitService rateLimitService;

    public ChatFacadeService(ChannelService channelService,
                              MessagingService messagingService,
                              EventPublisher eventPublisher,
                              MessageRateLimitService rateLimitService) {
        this.channelService = channelService;
        this.messagingService = messagingService;
        this.eventPublisher = eventPublisher;
        this.rateLimitService = rateLimitService;
    }

    public Message sendMessage(UUID channelId, UUID senderId, String content) {
        if (!rateLimitService.tryAcquire(senderId)) {
            throw new RateLimitedException();
        }

        Channel channel = channelService.getChannelOrThrow(channelId);
        channelService.requireMember(channelId, senderId);

        Message saved = messagingService.saveMessage(channelId, senderId, content, channel.isArchived());

        ChatMessageEvent event = new ChatMessageEvent(
                saved.getId(), saved.getChannelId(), saved.getSenderId(), saved.getContent(), saved.getSentAt());
        eventPublisher.publish(KafkaTopics.CHAT_MESSAGES, channelId.toString(), event);

        return saved;
    }
}
