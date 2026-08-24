package com.quickchat.backend.websocket;

import com.quickchat.backend.exception.NotAMemberException;
import com.quickchat.backend.security.JwtTokenProvider;
import com.quickchat.backend.service.ChannelService;
import com.quickchat.backend.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 회귀 테스트 - Build and Test 단계 보안 점검(H2)에서 발견한 버그: SUBSCRIBE 프레임에 인가 검사가 없어
 * 인증된 사용자라면 자신이 속하지 않은 채널의 실시간 메시지를 그대로 구독/엿볼 수 있었다. SUBSCRIBE 시점에
 * ChannelService.requireMember로 멤버십을 확인하도록 수정했다.
 */
@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PresenceService presenceService;
    @Mock
    private ChannelService channelService;

    private StompAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompAuthChannelInterceptor(jwtTokenProvider, presenceService, channelService);
    }

    @Test
    void subscribingToChannelTopicAsNonMemberIsRejected() {
        UUID channelId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        doThrow(new NotAMemberException()).when(channelService).requireMember(channelId, userId);

        Message<byte[]> subscribeFrame = subscribeMessage(channelId, userId);

        assertThatThrownBy(() -> interceptor.preSend(subscribeFrame, mock(MessageChannel.class)))
                .isInstanceOf(NotAMemberException.class);
    }

    @Test
    void subscribingToChannelTopicAsMemberIsAllowedThrough() {
        UUID channelId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        // channelService.requireMember는 멤버일 때 아무것도 던지지 않는다(default Mockito 동작) - 통과해야 함.

        Message<byte[]> subscribeFrame = subscribeMessage(channelId, userId);
        Message<?> result = interceptor.preSend(subscribeFrame, mock(MessageChannel.class));

        verify(channelService).requireMember(channelId, userId);
        org.assertj.core.api.Assertions.assertThat(result).isSameAs(subscribeFrame);
    }

    @Test
    void subscribingWithoutAnAuthenticatedUserIsRejected() {
        UUID channelId = UUID.randomUUID();
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/channel/" + channelId);
        Message<byte[]> subscribeFrame = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor.preSend(subscribeFrame, mock(MessageChannel.class)))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void subscribingToAnUnrelatedDestinationSkipsMembershipCheck() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/something-else");
        Message<byte[]> subscribeFrame = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(subscribeFrame, mock(MessageChannel.class));

        verify(channelService, org.mockito.Mockito.never()).requireMember(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private Message<byte[]> subscribeMessage(UUID channelId, UUID userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/channel/" + channelId);
        accessor.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
