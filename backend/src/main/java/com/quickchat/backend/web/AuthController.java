package com.quickchat.backend.web;

import com.quickchat.backend.service.AuthService;
import com.quickchat.backend.web.dto.LoginRequest;
import com.quickchat.backend.web.dto.RefreshRequest;
import com.quickchat.backend.web.dto.RegisterRequest;
import com.quickchat.backend.web.dto.TokenResponse;
import com.quickchat.backend.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** FR-1: 회원가입/로그인. story 1.1. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        var user = authService.register(request.email(), request.password(), request.displayName());
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(authService.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(authService.refreshAccessToken(request.refreshToken()));
    }
}
