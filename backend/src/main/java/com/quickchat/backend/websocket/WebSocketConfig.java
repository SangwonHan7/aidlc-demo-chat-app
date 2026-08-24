package com.quickchat.backend.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket 설정. tech-env.md: 폴링 미사용, WebSocket(STOMP)만 사용.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor authChannelInterceptor;
    private final String allowedOrigin;

    public WebSocketConfig(StompAuthChannelInterceptor authChannelInterceptor,
                            @Value("${quickchat.cors.allowed-origin}") String allowedOrigin) {
        this.authChannelInterceptor = authChannelInterceptor;
        this.allowedOrigin = allowedOrigin;
    }

    /**
     * Build and Test 보안 점검(M1)에서 발견: REST 쪽은 SecurityConfig의 CorsConfigurationSource로
     * 단일 origin만 허용하는데, WebSocket 쪽은 setAllowedOriginPatterns("*")로 모든 origin을 허용하고
     * 있어 두 쪽의 신뢰 경계가 불일치했다. NFR Design(Question 4 답변 B)이 이미 "Frontend는 정확히 하나의
     * origin에서 배포된다"고 결정했으므로, REST와 동일한 quickchat.cors.allowed-origin 값으로 좁혔다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigin);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
