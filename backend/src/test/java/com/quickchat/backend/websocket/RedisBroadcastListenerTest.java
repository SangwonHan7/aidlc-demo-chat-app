package com.quickchat.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.quickchat.backend.kafka.ChatMessageEvent;
import com.quickchat.backend.web.dto.MessageResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 회귀 테스트 - Build and Test 단계의 정적 계약 감사에서 발견한 버그: RedisBroadcastListener가
 * ChatMessageEvent(내부 Kafka 전송용, 필드명 messageId)를 변환 없이 그대로 STOMP로 내보내고 있어,
 * Frontend(id 필드를 기대)에는 실시간 메시지의 id가 항상 undefined로 도착해 두 번째 메시지부터
 * 화면에서 사라지는 문제가 있었다(chatStore.mergeIncomingMessage가 undefined===undefined로
 * 오인해 중복 제거). MessageResponse.from(ChatMessageEvent)로 변환해 내보내도록 수정했다.
 */
class RedisBroadcastListenerTest {

    @Test
    void broadcastsMessageResponseWithIdFieldInsteadOfRawEventWithMessageIdField() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisBroadcastListener listener = new RedisBroadcastListener(messagingTemplate, objectMapper);

        UUID messageId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        ChatMessageEvent event = new ChatMessageEvent(messageId, channelId, senderId, "hello", Instant.now());
        byte[] body = objectMapper.writeValueAsBytes(event);

        Message redisMessage = mock(Message.class);
        when(redisMessage.getBody()).thenReturn(body);
        when(redisMessage.getChannel())
                .thenReturn(("ws-broadcast:" + channelId).getBytes(StandardCharsets.UTF_8));

        listener.onMessage(redisMessage, null);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/channel/" + channelId), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).isInstanceOf(MessageResponse.class);
        MessageResponse response = (MessageResponse) payloadCaptor.getValue();
        assertThat(response.id()).isEqualTo(messageId);
        assertThat(response.channelId()).isEqualTo(channelId);
        assertThat(response.senderId()).isEqualTo(senderId);
        assertThat(response.content()).isEqualTo("hello");
    }
}
