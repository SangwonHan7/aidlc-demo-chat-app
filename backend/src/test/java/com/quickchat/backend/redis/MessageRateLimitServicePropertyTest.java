package com.quickchat.backend.redis;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PBT (Rate Limiter Invariant, nfr-design-patterns.md 보완 항목):
 * 윈도우 내 N번째 호출까지는 허용되고 N+1번째부터는 항상 차단된다.
 */
class MessageRateLimitServicePropertyTest {

    @Property
    @Label("제한(limit)까지는 허용되고 그 다음 호출부터는 항상 차단된다")
    @SuppressWarnings("unchecked")
    void allowsExactlyLimitCallsThenBlocks(@ForAll @IntRange(min = 1, max = 20) int limit,
                                            @ForAll @IntRange(min = 1, max = 30) int attempts) {
        Map<String, Long> counters = new HashMap<>();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(any())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            long next = counters.merge(key, 1L, Long::sum);
            return next;
        });

        MessageRateLimitService service = new MessageRateLimitService(redis, limit);
        UUID userId = UUID.randomUUID();

        int allowedCount = 0;
        for (int i = 0; i < attempts; i++) {
            if (service.tryAcquire(userId)) {
                allowedCount++;
            }
        }

        // 같은 (userId, 현재 초) 윈도우 안에서 호출했다고 가정할 때 허용 횟수는 min(limit, attempts)를 넘지 않는다.
        assertThat(allowedCount).isLessThanOrEqualTo(Math.min(limit, attempts));
        // limit번째 호출까지는 반드시 허용된다 (attempts >= limit인 경우).
        if (attempts >= limit) {
            assertThat(allowedCount).isEqualTo(limit);
        }
    }
}
