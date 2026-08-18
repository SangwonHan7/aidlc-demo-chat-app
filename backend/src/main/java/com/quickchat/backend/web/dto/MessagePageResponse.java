package com.quickchat.backend.web.dto;

import java.time.Instant;
import java.util.List;

/** nextCursor를 다음 요청의 before 파라미터로 넘기면 이어서 조회할 수 있다 (cursor 기반). */
public record MessagePageResponse(List<MessageResponse> messages, Instant nextCursor) {
}
