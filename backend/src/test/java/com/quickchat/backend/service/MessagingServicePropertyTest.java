package com.quickchat.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickchat.backend.domain.Message;
import com.quickchat.backend.exception.ChannelArchivedException;
import com.quickchat.backend.repository.MessageRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PBT-01 Testable Properties (MessagingComponent):
 * - Round-trip: 저장한 메시지는 조회 결과에 동일하게 나타난다.
 * - Invariant: ARCHIVED 채널에는 내용과 무관하게 항상 저장이 거부된다.
 */
class MessagingServicePropertyTest {

    @Property
    @Label("저장한 메시지 내용은 조회 결과에 그대로 나타난다 (round-trip)")
    void saveThenRetrieveRoundTrip(@ForAll @StringLength(min = 1, max = 500) String content) {
        List<Message> store = new ArrayList<>();
        MessageRepository repo = mock(MessageRepository.class);
        when(repo.save(any())).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            store.add(m);
            return m;
        });
        when(repo.findPage(any(), any(), any())).thenAnswer(inv -> {
            UUID channelId = inv.getArgument(0);
            return store.stream()
                    .filter(m -> m.getChannelId().equals(channelId))
                    .sorted(Comparator.comparing(Message::getSentAt).reversed())
                    .toList();
        });

        MessagingService service = new MessagingService(repo, mock(StringRedisTemplate.class), new ObjectMapper());
        UUID channelId = UUID.randomUUID();

        Message saved = service.saveMessage(channelId, UUID.randomUUID(), content, false);
        List<Message> history = service.getMessageHistory(channelId, null, 10);

        assertThat(history).extracting(Message::getContent).contains(saved.getContent());
        assertThat(history).extracting(Message::getId).contains(saved.getId());
    }

    @Property
    @Label("ARCHIVED 채널로의 저장 시도는 내용과 관계없이 항상 거부된다")
    void archivedChannelAlwaysRejectsSave(@ForAll @StringLength(min = 0, max = 500) String content) {
        MessageRepository repo = mock(MessageRepository.class);
        MessagingService service = new MessagingService(repo, mock(StringRedisTemplate.class), new ObjectMapper());

        assertThatThrownBy(() -> service.saveMessage(UUID.randomUUID(), UUID.randomUUID(), content, true))
                .isInstanceOf(ChannelArchivedException.class);
    }
}
