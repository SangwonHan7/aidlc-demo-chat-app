package com.quickchat.backend.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * User entity. See aidlc-docs/construction/backend/functional-design/domain-entities.md
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    // NOTE: 로그인 실패 카운터/잠금 상태는 JPA 컬럼이 아니라 Redis(LoginLockRedisService)에 저장합니다.
    // 근거: nfr-requirements.md Q2 답변 A - 멀티 파드 환경에서 상태를 공유해야 하기 때문입니다.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected User() {
        // JPA
    }

    public User(String email, String passwordHash, String displayName) {
        this.id = UUID.randomUUID(); // 클라이언트 측 ID 생성 (Channel.java와 동일한 패턴)
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
