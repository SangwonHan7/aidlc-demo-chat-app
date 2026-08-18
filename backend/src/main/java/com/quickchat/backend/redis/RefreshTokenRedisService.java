package com.quickchat.backend.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh Token 저장소. nfr-requirements.md Q3 답변 B - Redis에 저장(TTL 자동 만료).
 * 키: refresh-token:{token} -> value: userId
 */
@Service
public class RefreshTokenRedisService {

    private static final String KEY_PREFIX = "refresh-token:";

    private final StringRedisTemplate redis;
    private final Duration ttl;

    public RefreshTokenRedisService(
            StringRedisTemplate redis,
            @Value("${quickchat.jwt.refresh-token-ttl-days:14}") long ttlDays) {
        this.redis = redis;
        this.ttl = Duration.ofDays(ttlDays);
    }

    public void store(String refreshToken, UUID userId) {
        redis.opsForValue().set(key(refreshToken), userId.toString(), ttl);
    }

    public Optional<UUID> resolveUserId(String refreshToken) {
        String value = redis.opsForValue().get(key(refreshToken));
        return Optional.ofNullable(value).map(UUID::fromString);
    }

    public void revoke(String refreshToken) {
        redis.delete(key(refreshToken));
    }

    private String key(String refreshToken) {
        return KEY_PREFIX + refreshToken;
    }
}
