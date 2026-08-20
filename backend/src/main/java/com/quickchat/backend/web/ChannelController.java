package com.quickchat.backend.web;

import com.quickchat.backend.domain.ChannelType;
import com.quickchat.backend.service.ChannelService;
import com.quickchat.backend.web.dto.ChannelMemberResponse;
import com.quickchat.backend.web.dto.ChannelResponse;
import com.quickchat.backend.web.dto.CreateChannelRequest;
import com.quickchat.backend.web.dto.InviteMemberRequest;
import com.quickchat.backend.web.dto.StartDirectChannelRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/** FR-3, FR-4: 그룹 채널 생성/참여/관리 + 1:1 DM 채널. story 1.3, 2.1, 2.2. */
@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelResponse create(@Valid @RequestBody CreateChannelRequest request, Principal principal) {
        var channel = channelService.createChannel(
                request.name(), ChannelType.GROUP, request.visibility(), currentUserId(principal));
        return ChannelResponse.from(channel);
    }

    @PostMapping("/direct")
    public ChannelResponse startDirect(@Valid @RequestBody StartDirectChannelRequest request, Principal principal) {
        var channel = channelService.getOrCreateDirectChannel(currentUserId(principal), request.otherUserId());
        return ChannelResponse.from(channel);
    }

    @GetMapping
    public List<ChannelResponse> listMine(Principal principal) {
        return channelService.listChannelsForUser(currentUserId(principal)).stream()
                .map(ChannelResponse::from).toList();
    }

    /**
     * 참여 여부와 무관한 PUBLIC 그룹 채널 전체 목록 (story 1.3). Frontend Code Generation 중 발견된
     * 누락 보완 - 기존 GET /api/channels는 "내 채널"만 반환해 아직 참여하지 않은 공개 채널을 발견할
     * 방법이 없었다.
     */
    @GetMapping("/discoverable")
    public List<ChannelResponse> listDiscoverable() {
        return channelService.listDiscoverablePublicChannels().stream()
                .map(ChannelResponse::from).toList();
    }

    @PostMapping("/{channelId}/join")
    public void join(@PathVariable UUID channelId, Principal principal) {
        channelService.joinChannel(channelId, currentUserId(principal));
    }

    @PostMapping("/{channelId}/members")
    public void invite(@PathVariable UUID channelId, @Valid @RequestBody InviteMemberRequest request,
                        Principal principal) {
        channelService.inviteMember(channelId, currentUserId(principal), request.inviteeId());
    }

    @DeleteMapping("/{channelId}/members/{userId}")
    public void removeMember(@PathVariable UUID channelId, @PathVariable UUID userId, Principal principal) {
        channelService.removeMember(channelId, currentUserId(principal), userId);
    }

    /**
     * 채널 멤버 목록 조회 (요청자도 멤버여야 함). Frontend Code Generation 중 발견된 누락 보완 -
     * 멤버 관리 UI(story 2.2)와 DM 상대 식별(story 1.2/1.4)에 필요.
     */
    @GetMapping("/{channelId}/members")
    public List<ChannelMemberResponse> listMembers(@PathVariable UUID channelId, Principal principal) {
        return channelService.listMembers(channelId, currentUserId(principal)).stream()
                .map(ChannelMemberResponse::from).toList();
    }

    private UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
