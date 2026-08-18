package com.quickchat.backend.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 사용자당 메시지 전송 rate limit. nfr-design-patterns.md Q4 답변 A.
 * 고정 윈도우(1초) 카운터. 키: rate-limit:{userId}:{epochSecond}
 * Invariant: 윈도우 내 N번째 호출까지는 허용, N+1번째부터는 항상 차단 (business-logic-model.md PBT 보완 항목).
 */
@Service
public class MessageRateLimitService {

    private final StringRedisTemplate redis;
    private final int limitPerSecond;

    public MessageRateLimitService(
            StringRedisTemplate redis,
            @Value("${quickchat.rate-limit.messages-per-second:5}") int limitPerSecond) {
        this.redis = redis;
        this.limitPerSecond = limitPerSecond;
    }

    /** @return true=허용, false=차단(RATE_LIMITED) */
    public boolean tryAcquire(UUID userId) {
        String key = "rate-limit:" + userId + ":" + Instant.now().getEpochSecond();
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, Duration.ofSeconds(2));
        }
        return count != null && count <= limitPerSecond;
    }
}
