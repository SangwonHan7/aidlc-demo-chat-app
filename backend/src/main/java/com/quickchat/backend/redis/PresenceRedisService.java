package com.quickchat.backend.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 온라인 상태 추적. 키: presence:{userId} -> 활성 세션 수(Set 대신 카운터로 단순화).
 * markOnline은 멱등적이다 (PBT: Idempotence, business-logic-model.md 참고).
 */
@Service
public class PresenceRedisService {

    private static final String KEY_PREFIX = "presence:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(2); // heartbeat로 갱신 필요

    private final StringRedisTemplate redis;

    public PresenceRedisService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 여러 번 호출해도 online 상태는 동일하게 유지된다 (Idempotence). */
    public void markOnline(UUID userId, String sessionId) {
        redis.opsForSet().add(key(userId), sessionId);
        redis.expire(key(userId), SESSION_TTL);
    }

    public void markOffline(UUID userId, String sessionId) {
        redis.opsForSet().remove(key(userId), sessionId);
    }

    public boolean isOnline(UUID userId) {
        Long size = redis.opsForSet().size(key(userId));
        return size != null && size > 0;
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
