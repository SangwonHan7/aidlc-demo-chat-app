package com.quickchat.backend.service;

import com.quickchat.backend.domain.User;
import com.quickchat.backend.exception.AccountLockedException;
import com.quickchat.backend.exception.EmailAlreadyExistsException;
import com.quickchat.backend.exception.InvalidCredentialsException;
import com.quickchat.backend.redis.LoginLockRedisService;
import com.quickchat.backend.redis.RefreshTokenRedisService;
import com.quickchat.backend.repository.UserRepository;
import com.quickchat.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** business-logic-model.md AuthComponent 워크플로우의 예시 기반 테스트. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private LoginLockRedisService loginLock;
    @Mock
    private RefreshTokenRedisService refreshTokens;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider("unit-test-secret-key-must-be-32-bytes-min", 15);

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, loginLock, refreshTokens, jwtTokenProvider);
    }

    @Test
    void registerFailsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("dup@example.com", "password123", "Dup"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerSavesUserWithHashedPassword() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User created = authService.register("new@example.com", "password123", "New User");

        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(passwordEncoder.matches("password123", created.getPasswordHash())).isTrue();
    }

    @Test
    void loginFailsImmediatelyWhenAccountIsLocked() {
        when(loginLock.isLocked("locked@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("locked@example.com", "whatever"))
                .isInstanceOf(AccountLockedException.class);

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void loginWithWrongPasswordRecordsFailureAndThrowsInvalidCredentials() {
        User user = new User("user@example.com", passwordEncoder.encode("correct-password"), "User");
        when(loginLock.isLocked("user@example.com")).thenReturn(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(loginLock.recordFailure("user@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("user@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(loginLock).recordFailure("user@example.com");
    }

    @Test
    void loginWithWrongPasswordOnFifthAttemptThrowsAccountLocked() {
        User user = new User("user@example.com", passwordEncoder.encode("correct-password"), "User");
        when(loginLock.isLocked("user@example.com")).thenReturn(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(loginLock.recordFailure("user@example.com")).thenReturn(true); // 5번째 실패

        assertThatThrownBy(() -> authService.login("user@example.com", "wrong-password"))
                .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void loginWithCorrectPasswordResetsFailureCountAndReturnsTokens() {
        User user = new User("user@example.com", passwordEncoder.encode("correct-password"), "User");
        when(loginLock.isLocked("user@example.com")).thenReturn(false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        TokenPair tokens = authService.login("user@example.com", "correct-password");

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        verify(loginLock).reset("user@example.com");
        verify(refreshTokens).store(eq(tokens.refreshToken()), any(UUID.class));
    }
}
