package com.quickchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickchat.backend.domain.Message;
import com.quickchat.backend.exception.ChannelArchivedException;
import com.quickchat.backend.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** business-rules.md, business-logic-model.md MessagingComponent 워크플로우의 예시 기반 테스트. */
@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private StringRedisTemplate redis;

    private MessagingService messagingService;

    @BeforeEach
    void setUp() {
        messagingService = new MessagingService(messageRepository, redis, new ObjectMapper());
    }

    @Test
    void savingToArchivedChannelThrows() {
        assertThatThrownBy(() -> messagingService.saveMessage(
                UUID.randomUUID(), UUID.randomUUID(), "hello", true))
                .isInstanceOf(ChannelArchivedException.class);

        verify(messageRepository, never()).save(any());
    }

    @Test
    void savingToActiveChannelPersistsMessage() {
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        UUID channelId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();

        Message saved = messagingService.saveMessage(channelId, senderId, "hello", false);

        assertThat(saved.getChannelId()).isEqualTo(channelId);
        assertThat(saved.getSenderId()).isEqualTo(senderId);
        assertThat(saved.getContent()).isEqualTo("hello");
    }

    @Test
    void getMessageHistoryDelegatesToRepositoryWithDefaultPageSize() {
        UUID channelId = UUID.randomUUID();
        messagingService.getMessageHistory(channelId, null, 0);

        verify(messageRepository).findPage(eq(channelId), isNull(),
                argThat(pageable -> pageable.getPageSize() == 50));
    }
}
