package com.quickchat.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT Access Token 발급/검증. 시크릿은 Vault에서 조회되어 quickchat.jwt.secret으로 주입된다
 * (requirements.md 기본 보안 요구사항).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenTtlMinutes;

    public JwtTokenProvider(
            @Value("${quickchat.jwt.secret}") String secret,
            @Value("${quickchat.jwt.access-token-ttl-minutes:15}") long accessTokenTtlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    public String generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenTtlMinutes * 60)))
                .signWith(key)
                .compact();
    }

    /** 라운드트립 속성: login(register)에서 발급한 토큰은 항상 유효하게 검증되어야 한다 (PBT). */
    public Optional<UUID> validateAndGetUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
