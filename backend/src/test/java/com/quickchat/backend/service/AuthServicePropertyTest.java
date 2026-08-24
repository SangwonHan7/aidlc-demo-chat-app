package com.quickchat.backend.service;

import com.quickchat.backend.redis.LoginLockRedisService;
import com.quickchat.backend.redis.RefreshTokenRedisService;
import com.quickchat.backend.repository.UserRepository;
import com.quickchat.backend.security.JwtTokenProvider;
import net.jqwik.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PBT-01 Testable Property (AuthComponent, Round-trip):
 * "login(register(email,pw)) 성공" - business-logic-model.md 참고.
 * PBT-07 준수: 도메인에 맞는 커스텀 생성기(이메일/비밀번호 형태)를 사용한다.
 */
class AuthServicePropertyTest {

    @Property(tries = 30)
    @Label("register 직후 동일한 비밀번호로 login이 항상 성공한다 (round-trip)")
    void registerThenLoginRoundTrip(@ForAll("validEmails") String email,
                                     @ForAll("validPasswords") String password,
                                     @ForAll("validDisplayNames") String displayName) {
        UserRepository userRepository = mock(UserRepository.class);
        Map<String, com.quickchat.backend.domain.User> byEmail = new HashMap<>();
        when(userRepository.existsByEmail(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> byEmail.containsKey(inv.<String>getArgument(0)));
        when(userRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> {
                    com.quickchat.backend.domain.User u = inv.getArgument(0);
                    byEmail.put(u.getEmail(), u);
                    return u;
                });
        when(userRepository.findByEmail(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> Optional.ofNullable(byEmail.get(inv.<String>getArgument(0))));

        LoginLockRedisService loginLock = mock(LoginLockRedisService.class); // 기본: locked=false
        RefreshTokenRedisService refreshTokens = mock(RefreshTokenRedisService.class);
        JwtTokenProvider jwtTokenProvider =
                new JwtTokenProvider("property-test-secret-key-32-bytes-minimum-len", 15);

        AuthService authService = new AuthService(
                userRepository, new BCryptPasswordEncoder(4), loginLock, refreshTokens, jwtTokenProvider);

        authService.register(email, password, displayName);
        TokenPair tokens = authService.login(email, password);

        assertThat(tokens.accessToken()).isNotBlank();
    }

    @Provide
    Arbitrary<String> validEmails() {
        Arbitrary<String> local = Arbitraries.strings().withCharRange('a', 'z').ofMinLength(3).ofMaxLength(12);
        Arbitrary<String> domain = Arbitraries.of("example.com", "quickchat.test", "mail.co.kr");
        return Combinators.combine(local, domain).as((l, d) -> l + "@" + d);
    }

    @Provide
    Arbitrary<String> validPasswords() {
        return Arbitraries.strings().withCharRange('a', 'z').numeric().ofMinLength(8).ofMaxLength(30);
    }

    @Provide
    Arbitrary<String> validDisplayNames() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(50);
    }
}
