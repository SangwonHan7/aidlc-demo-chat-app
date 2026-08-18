package com.quickchat.backend.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 로그인 실패 카운터/잠금 상태. nfr-requirements.md Q2 답변 A - Redis에 저장(멀티 파드 공유).
 * 키: login-lock:{email}
 */
@Service
public class LoginLockRedisService {

    private static final String KEY_PREFIX = "login-lock:";

    private final StringRedisTemplate redis;
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginLockRedisService(
            StringRedisTemplate redis,
            @Value("${quickchat.security.login-lock.max-attempts:5}") int maxAttempts,
            @Value("${quickchat.security.login-lock.lock-duration-minutes:15}") long lockDurationMinutes) {
        this.redis = redis;
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockDurationMinutes);
    }

    public boolean isLocked(String email) {
        String value = redis.opsForValue().get(key(email));
        return value != null && Integer.parseInt(value) >= maxAttempts;
    }

    /** @return 이번 실패로 잠금 임계값에 도달했는지 여부 */
    public boolean recordFailure(String email) {
        Long count = redis.opsForValue().increment(key(email));
        if (count != null && count == 1L) {
            // 첫 실패 시 카운터에 TTL을 걸어, 락 없이 방치된 카운터가 무한히 누적되지 않게 한다.
            redis.expire(key(email), lockDuration);
        }
        if (count != null && count == maxAttempts) {
            // 잠금 임계값에 도달한 순간부터 lockDuration을 다시 시작 (잠금 시간을 온전히 보장).
            redis.expire(key(email), lockDuration);
        }
        return count != null && count >= maxAttempts;
    }

    public void reset(String email) {
        redis.delete(key(email));
    }

    private String key(String email) {
        return KEY_PREFIX + email;
    }
}
