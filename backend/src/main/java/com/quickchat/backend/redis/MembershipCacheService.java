package com.quickchat.backend.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 채널 멤버십 캐시. nfr-design-patterns.md Q3 답변 A.
 * 키: membership:{channelId}:{userId} -> "1" (멤버) - 짧은 TTL, 변경 시 즉시 무효화.
 * 캐시 미스(null) 시 호출자는 DB를 조회해 진실을 확인해야 한다 (캐시는 positive-cache로만 사용).
 */
@Service
public class MembershipCacheService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redis;

    public MembershipCacheService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void cacheMember(UUID channelId, UUID userId) {
        redis.opsForValue().set(key(channelId, userId), "1", TTL);
    }

    /** true=캐시에서 멤버 확인됨, false=캐시 미스(멤버가 아니라는 뜻은 아님 - DB 확인 필요) */
    public boolean isCachedMember(UUID channelId, UUID userId) {
        return "1".equals(redis.opsForValue().get(key(channelId, userId)));
    }

    public void invalidate(UUID channelId, UUID userId) {
        redis.delete(key(channelId, userId));
    }

    private String key(UUID channelId, UUID userId) {
        return "membership:" + channelId + ":" + userId;
    }
}
