package com.quickchat.backend.web;

import com.quickchat.backend.service.PresenceService;
import com.quickchat.backend.web.dto.PresenceStatusResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * FR-6: 온라인 상태 조회. story 1.4.
 * frontend-functional-design-clarification-questions.md Gap 2에서 발견된 누락을 보완:
 * 기존에는 PresenceService/PresenceRedisService만 존재하고 이를 노출하는 API가 없었음.
 */
@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping
    public List<PresenceStatusResponse> getPresence(@RequestParam List<UUID> userIds) {
        return userIds.stream()
                .map(id -> new PresenceStatusResponse(id, presenceService.isOnline(id)))
                .toList();
    }
}
