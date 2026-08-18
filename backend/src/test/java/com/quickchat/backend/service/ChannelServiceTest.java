package com.quickchat.backend.service;

import com.quickchat.backend.domain.*;
import com.quickchat.backend.exception.AlreadyMemberException;
import com.quickchat.backend.exception.ForbiddenActionException;
import com.quickchat.backend.redis.MembershipCacheService;
import com.quickchat.backend.repository.ChannelMemberRepository;
import com.quickchat.backend.repository.ChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** business-rules.md, business-logic-model.md ChannelComponent 워크플로우의 예시 기반 테스트. */
@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ChannelMemberRepository memberRepository;
    @Mock
    private MembershipCacheService membershipCache;

    private ChannelService channelService;

    @BeforeEach
    void setUp() {
        channelService = new ChannelService(channelRepository, memberRepository, membershipCache);
        when(channelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createChannelAddsOwnerAsMember() {
        UUID ownerId = UUID.randomUUID();

        Channel channel = channelService.createChannel("team-chat", ChannelType.GROUP, ChannelVisibility.PUBLIC, ownerId);

        assertThat(channel.getOwnerId()).isEqualTo(ownerId);
        verify(memberRepository).save(argThat(m -> m.getUserId().equals(ownerId) && m.getRole() == ChannelRole.OWNER));
        verify(membershipCache).cacheMember(channel.getId(), ownerId);
    }

    @Test
    void joiningTwiceThrowsAlreadyMember() {
        UUID channelId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Channel channel = new Channel("public-room", ChannelType.GROUP, ChannelVisibility.PUBLIC, UUID.randomUUID());
        when(channelRepository.findById(channelId)).thenReturn(java.util.Optional.of(channel));
        when(membershipCache.isCachedMember(channelId, userId)).thenReturn(true); // 이미 멤버로 캐시됨

        assertThatThrownBy(() -> channelService.joinChannel(channelId, userId))
                .isInstanceOf(AlreadyMemberException.class);
    }

    @Test
    void nonOwnerCannotInviteMembers() {
        UUID channelId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID nonOwnerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        Channel channel = new Channel("private-room", ChannelType.GROUP, ChannelVisibility.INVITE_ONLY, ownerId);
        when(channelRepository.findById(channelId)).thenReturn(java.util.Optional.of(channel));
        when(memberRepository.findByChannelId(channelId)).thenReturn(
                List.of(new ChannelMember(channelId, ownerId, ChannelRole.OWNER),
                        new ChannelMember(channelId, nonOwnerId, ChannelRole.MEMBER)));

        assertThatThrownBy(() -> channelService.inviteMember(channelId, nonOwnerId, inviteeId))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void ownerLeavingArchivesChannel() {
        UUID channelId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Channel channel = new Channel("team-chat", ChannelType.GROUP, ChannelVisibility.PUBLIC, ownerId);
        when(channelRepository.findById(channelId)).thenReturn(java.util.Optional.of(channel));

        channelService.removeMember(channelId, ownerId, ownerId);

        assertThat(channel.isArchived()).isTrue();
        verify(channelRepository).save(channel);
    }

    @Test
    void memberLeavingDoesNotArchiveChannel() {
        UUID channelId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Channel channel = new Channel("team-chat", ChannelType.GROUP, ChannelVisibility.PUBLIC, ownerId);
        when(channelRepository.findById(channelId)).thenReturn(java.util.Optional.of(channel));

        channelService.removeMember(channelId, memberId, memberId);

        assertThat(channel.isArchived()).isFalse();
        verify(channelRepository, never()).save(any());
    }
}
