package com.quickchat.backend.web.dto;

/** 표준 에러 응답. tech-env.md: { "errorCode": "...", "message": "..." } */
public record ErrorResponse(String errorCode, String message) {
}
