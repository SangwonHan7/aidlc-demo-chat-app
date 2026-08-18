package com.quickchat.backend.websocket;

import com.quickchat.backend.service.ChatFacadeService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

/**
 * WebSocket(STOMP) 메시지 전송 엔드포인트. tech-env.md의 예시 코드와 동일한 패턴을 따른다.
 * 클라이언트는 /app/chat.send/{channelId}로 전송하고, /topic/channel/{channelId}를 구독한다.
 */
@Controller
public class ChatWebSocketController {

    private final ChatFacadeService chatFacadeService;

    public ChatWebSocketController(ChatFacadeService chatFacadeService) {
        this.chatFacadeService = chatFacadeService;
    }

    @MessageMapping("/chat.send/{channelId}")
    public void sendMessage(@DestinationVariable String channelId,
                             @Payload SendMessageStompRequest request,
                             Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        chatFacadeService.sendMessage(UUID.fromString(channelId), senderId, request.content());
        // 응답은 @SendTo가 아니라 RedisBroadcastListener가 /topic/channel/{channelId}로 비동기 전달한다
        // (Kafka를 경유하는 저장-후-발행 구조이므로 즉시 @SendTo 응답은 사용하지 않는다).
    }
}
