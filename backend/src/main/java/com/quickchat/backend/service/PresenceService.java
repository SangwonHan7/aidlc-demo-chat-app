package com.quickchat.backend.service;

import com.quickchat.backend.redis.PresenceRedisService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** PresenceComponent 구현. 상태 저장은 PresenceRedisService(Redis)에 위임한다. */
@Service
public class PresenceService {

    private final PresenceRedisService presenceRedis;

    public PresenceService(PresenceRedisService presenceRedis) {
        this.presenceRedis = presenceRedis;
    }

    public void markOnline(UUID userId, String sessionId) {
        presenceRedis.markOnline(userId, sessionId);
    }

    public void markOffline(UUID userId, String sessionId) {
        presenceRedis.markOffline(userId, sessionId);
    }

    public boolean isOnline(UUID userId) {
        return presenceRedis.isOnline(userId);
    }
}
