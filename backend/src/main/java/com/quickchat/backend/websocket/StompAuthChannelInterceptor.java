package com.quickchat.backend.websocket;

import com.quickchat.backend.security.JwtTokenProvider;
import com.quickchat.backend.service.PresenceService;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * STOMP CONNECT 프레임의 Authorization 헤더에서 JWT를 꺼내 검증하고,
 * 세션의 Principal로 설정한다. 이후 @MessageMapping에서 Principal로 인증된 사용자를 얻을 수 있다.
 *
 * <p>Story 1.4(온라인 상태): CONNECT 성공 시 markOnline, DISCONNECT 시 markOffline을 호출해
 * PresenceRedisService에 세션 단위로 반영한다 (frontend-functional-design-clarification-questions.md
 * Gap 2에서 발견된 누락 보완). Spring은 CONNECT 때 설정한 Principal을 세션에 보관해 이후 프레임의
 * accessor.getUser()로 그대로 돌려주므로, DISCONNECT 시에도 별도 조회 없이 사용자 식별이 가능하다.
 *
 * <p>알려진 제약: presence:{userId} Redis 키의 TTL(2분)을 하트비트로 갱신하는 로직은 아직 없음 -
 * WebSocketConfig에 STOMP 하트비트가 설정되어 있지 않아 이번 최소 보완 범위에서는 제외했고,
 * Build and Test 단계에서 장시간 연결 시나리오로 검증/보완이 필요하다.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final PresenceService presenceService;

    public StompAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider, PresenceService presenceService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.presenceService = presenceService;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = firstNativeHeader(accessor, "Authorization");
            String token = (authHeader != null && authHeader.startsWith("Bearer "))
                    ? authHeader.substring(7) : authHeader;

            UUID userId = token == null ? null : jwtTokenProvider.validateAndGetUserId(token).orElse(null);
            if (userId == null) {
                throw new org.springframework.messaging.MessagingException("Invalid or missing WebSocket auth token");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
            presenceService.markOnline(userId, accessor.getSessionId());
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (user != null) {
                presenceService.markOffline(UUID.fromString(user.getName()), accessor.getSessionId());
            }
        }
        return message;
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }
}
