package com.quickchat.backend.web.dto;

import com.quickchat.backend.service.TokenPair;

public record TokenResponse(String accessToken, String refreshToken) {
    public static TokenResponse from(TokenPair pair) {
        return new TokenResponse(pair.accessToken(), pair.refreshToken());
    }
}
