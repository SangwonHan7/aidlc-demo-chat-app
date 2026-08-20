package com.quickchat.backend.web;

import com.quickchat.backend.exception.UserNotFoundException;
import com.quickchat.backend.repository.UserRepository;
import com.quickchat.backend.web.dto.UserResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * 사용자 프로필 일괄 조회. Frontend Code Generation 중 발견된 누락 보완 - 채널 멤버 목록/DM 상대의
 * displayName·email을 화면에 표시하려면 UUID를 UserResponse로 변환해야 하는데, 그런 조회 API가 없었음.
 * 인증만 요구하고(SecurityConfig 기본 규칙), 별도의 멤버십 검증은 하지 않는다 - 이미 멤버 목록/DM으로
 * UUID를 알게 된 상대의 공개 프로필(이메일/표시이름)만 노출하므로 낮은 위험으로 판단.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 로그인/새로고침 후 "나는 누구인가"를 알아낼 방법이 전혀 없어 Frontend authStore.user가 항상 null로
     * 남는 문제를 Frontend Code Generation(AppShellLayout 부트스트랩 단계)에서 발견해 보완.
     * JwtAuthenticationFilter가 SecurityContext에 심어둔 Principal.getName()(=userId)을 그대로 사용한다
     * (ChannelController.currentUserId(Principal)과 동일한 패턴).
     */
    @GetMapping("/me")
    public UserResponse getCurrentUser(Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        return userRepository.findById(userId).map(UserResponse::from).orElseThrow(UserNotFoundException::new);
    }

    @GetMapping
    public List<UserResponse> getUsers(@RequestParam List<UUID> ids) {
        return userRepository.findAllById(ids).stream().map(UserResponse::from).toList();
    }

    /**
     * 이메일 정확히 일치하는 사용자 검색 (story 1.2 "DM 상대 검색", story 2.2 "초대할 사용자 검색").
     * 부분/유사 검색은 지원하지 않음 - 임의 사용자 열람(enumeration) 범위를 넓히지 않기 위한 의도적 단순화.
     * (참고: 이미 회원가입 시 EMAIL_ALREADY_EXISTS 응답으로 이메일 존재 여부 자체는 노출되고 있어,
     * 이 엔드포인트가 새로운 정보를 추가로 노출하는 것은 아니다.)
     */
    @GetMapping("/search")
    public List<UserResponse> searchByEmail(@RequestParam String email) {
        return userRepository.findByEmail(email).map(UserResponse::from).map(List::of).orElse(List.of());
    }
}
