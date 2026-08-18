package com.quickchat.backend.service;

import com.quickchat.backend.domain.User;
import com.quickchat.backend.exception.AccountLockedException;
import com.quickchat.backend.exception.EmailAlreadyExistsException;
import com.quickchat.backend.exception.InvalidCredentialsException;
import com.quickchat.backend.exception.UserNotFoundException;
import com.quickchat.backend.redis.LoginLockRedisService;
import com.quickchat.backend.redis.RefreshTokenRedisService;
import com.quickchat.backend.repository.UserRepository;
import com.quickchat.backend.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * AuthComponent 구현. business-logic-model.md의 register/login 워크플로우 참고.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginLockRedisService loginLock;
    private final RefreshTokenRedisService refreshTokens;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        LoginLockRedisService loginLock,
                        RefreshTokenRedisService refreshTokens,
                        JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginLock = loginLock;
        this.refreshTokens = refreshTokens;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public User register(String email, String rawPassword, String displayName) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        User user = new User(email, passwordEncoder.encode(rawPassword), displayName);
        return userRepository.save(user);
    }

    /** round-trip 속성: register 직후 동일 비밀번호로 login이 성공해야 한다. */
    @Transactional
    public TokenPair login(String email, String rawPassword) {
        if (loginLock.isLocked(email)) {
            throw new AccountLockedException();
        }

        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            boolean nowLocked = loginLock.recordFailure(email);
            if (nowLocked) {
                throw new AccountLockedException();
            }
            throw new InvalidCredentialsException();
        }

        loginLock.reset(email);
        return issueTokenPair(user);
    }

    @Transactional
    public TokenPair refreshAccessToken(String refreshToken) {
        UUID userId = refreshTokens.resolveUserId(refreshToken)
                .orElseThrow(InvalidCredentialsException::new);
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        refreshTokens.revoke(refreshToken); // 회전(rotation): 기존 refresh token은 폐기
        return issueTokenPair(user);
    }

    private TokenPair issueTokenPair(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = UUID.randomUUID().toString();
        refreshTokens.store(refreshToken, user.getId());
        return new TokenPair(accessToken, refreshToken);
    }
}
