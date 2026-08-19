package com.quickchat.backend.web.dto;

import java.util.UUID;

/** Story 1.4: 온라인 상태 조회 응답. frontend-functional-design-clarification-questions.md Gap 2 해결. */
public record PresenceStatusResponse(UUID userId, boolean online) {
}
