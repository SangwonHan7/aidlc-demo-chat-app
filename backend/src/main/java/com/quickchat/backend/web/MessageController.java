package com.quickchat.backend.web;

import com.quickchat.backend.service.ChannelService;
import com.quickchat.backend.service.MessagingService;
import com.quickchat.backend.web.dto.MessagePageResponse;
import com.quickchat.backend.web.dto.MessageResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** FR-7: 채널/DM 메시지 이력 페이지 조회 (cursor 기반). story 1.5. */
@RestController
@RequestMapping("/api/channels/{channelId}/messages")
public class MessageController {

    private final ChannelService channelService;
    private final MessagingService messagingService;

    public MessageController(ChannelService channelService, MessagingService messagingService) {
        this.channelService = channelService;
        this.messagingService = messagingService;
    }

    @GetMapping
    public MessagePageResponse getHistory(
            @PathVariable UUID channelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(defaultValue = "50") int size,
            Principal principal) {

        channelService.requireMember(channelId, UUID.fromString(principal.getName()));

        List<MessageResponse> messages = messagingService.getMessageHistory(channelId, before, size).stream()
                .map(MessageResponse::from).toList();
        Instant nextCursor = messages.isEmpty() ? null : messages.get(messages.size() - 1).sentAt();
        return new MessagePageResponse(messages, nextCursor);
    }
}
