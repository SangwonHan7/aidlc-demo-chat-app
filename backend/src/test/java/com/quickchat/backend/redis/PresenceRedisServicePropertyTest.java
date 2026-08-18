package com.quickchat.backend.redis;

import net.jqwik.api.*;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PBT-01 Testable Property (PresenceComponent, Idempotence):
 * markOnline(markOnline(u)) 상태는 markOnline(u) 상태와 동일하다 - business-logic-model.md 참고.
 */
class PresenceRedisServicePropertyTest {

    @Property
    @Label("markOnline을 여러 번 호출해도 online 상태는 변하지 않는다 (idempotence)")
    @SuppressWarnings("unchecked")
    void markOnlineIsIdempotent(@ForAll @net.jqwik.api.constraints.IntRange(min = 1, max = 5) int repeatCount) {
        Set<String> backingSet = new HashSet<>();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SetOperations<String, String> setOps = mock(SetOperations.class);
        when(redis.opsForSet()).thenReturn(setOps);
        when(setOps.add(any(), any())).thenAnswer(inv -> {
            String value = inv.getArgument(1);
            return backingSet.add(value) ? 1L : 0L;
        });
        when(setOps.size(any())).thenAnswer(inv -> (long) backingSet.size());

        PresenceRedisService service = new PresenceRedisService(redis);
        UUID userId = UUID.randomUUID();
        String sessionId = "session-1";

        for (int i = 0; i < repeatCount; i++) {
            service.markOnline(userId, sessionId);
        }

        assertThat(backingSet).hasSize(1);
        assertThat(service.isOnline(userId)).isTrue();
    }
}
