package com.quickchat.backend.kafka;

import com.quickchat.backend.service.MessagingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * EventComponent의 구독(subscribe) 책임 구현.
 * chat-messages 토픽을 구독해 MessagingService.broadcastMessage()를 트리거한다
 * (component-dependency.md 메시지 전송 시퀀스 5단계).
 */
@Component
public class ChatMessageConsumer {

    private final MessagingService messagingService;

    public ChatMessageConsumer(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @KafkaListener(topics = KafkaTopics.CHAT_MESSAGES, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(ChatMessageEvent event) {
        messagingService.broadcastMessage(event);
    }
}
