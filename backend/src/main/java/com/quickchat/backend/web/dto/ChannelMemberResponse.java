package com.quickchat.backend.web.dto;

import com.quickchat.backend.domain.ChannelMember;
import com.quickchat.backend.domain.ChannelRole;

import java.util.UUID;

/**
 * Story 2.2(멤버 관리)/1.2(DM 상대 식별)에 필요한 채널 멤버 목록 응답.
 * Frontend Code Generation 중 발견된 누락 보완 - business-logic-summary.md(Frontend) 참고.
 */
public record ChannelMemberResponse(UUID userId, ChannelRole role) {
    public static ChannelMemberResponse from(ChannelMember member) {
        return new ChannelMemberResponse(member.getUserId(), member.getRole());
    }
}
