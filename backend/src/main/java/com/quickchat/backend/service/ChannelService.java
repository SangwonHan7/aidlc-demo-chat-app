package com.quickchat.backend.service;

import com.quickchat.backend.domain.*;
import com.quickchat.backend.exception.AlreadyMemberException;
import com.quickchat.backend.exception.ChannelNotFoundException;
import com.quickchat.backend.exception.ForbiddenActionException;
import com.quickchat.backend.exception.NotAMemberException;
import com.quickchat.backend.redis.MembershipCacheService;
import com.quickchat.backend.repository.ChannelMemberRepository;
import com.quickchat.backend.repository.ChannelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ChannelComponent 구현. business-rules.md, business-logic-model.md 참고.
 */
@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository memberRepository;
    private final MembershipCacheService membershipCache;

    public ChannelService(ChannelRepository channelRepository,
                           ChannelMemberRepository memberRepository,
                           MembershipCacheService membershipCache) {
        this.channelRepository = channelRepository;
        this.memberRepository = memberRepository;
        this.membershipCache = membershipCache;
    }

    @Transactional
    public Channel createChannel(String name, ChannelType type, ChannelVisibility visibility, UUID ownerId) {
        Channel channel = new Channel(name, type, visibility, ownerId);
        channel = channelRepository.save(channel);
        ChannelMember owner = new ChannelMember(channel.getId(), ownerId, ChannelRole.OWNER);
        memberRepository.save(owner);
        membershipCache.cacheMember(channel.getId(), ownerId);
        return channel;
    }

    /**
     * 1:1 DM 채널을 가져오거나 없으면 새로 만든다. DIRECT 채널은 정확히 멤버 2명을 가진다
     * (domain-entities.md). 별도의 "DM 채널 생성" API는 없고, 최초 메시지 전송 시 자동 생성된다
     * (business-rules.md).
     */
    @Transactional
    public Channel getOrCreateDirectChannel(UUID requesterId, UUID otherUserId) {
        List<UUID> requesterChannelIds = memberRepository.findByUserId(requesterId).stream()
                .map(ChannelMember::getChannelId).toList();
        for (UUID channelId : requesterChannelIds) {
            Channel channel = channelRepository.findById(channelId).orElse(null);
            if (channel != null && channel.getType() == ChannelType.DIRECT) {
                boolean otherIsMember = memberRepository.findByChannelId(channelId).stream()
                        .anyMatch(m -> m.getUserId().equals(otherUserId));
                if (otherIsMember) {
                    return channel;
                }
            }
        }
        Channel created = createChannel(
                "dm:" + requesterId + ":" + otherUserId, ChannelType.DIRECT, ChannelVisibility.INVITE_ONLY, requesterId);
        memberRepository.save(new ChannelMember(created.getId(), otherUserId, ChannelRole.MEMBER));
        membershipCache.cacheMember(created.getId(), otherUserId);
        return created;
    }

    /** PUBLIC 채널 자율 참여. 이미 멤버면 ALREADY_MEMBER (business-rules.md Q4 답변 A). */
    @Transactional
    public void joinChannel(UUID channelId, UUID userId) {
        Channel channel = getChannelOrThrow(channelId);
        requireNotAlreadyMember(channelId, userId);
        memberRepository.save(new ChannelMember(channelId, userId, ChannelRole.MEMBER));
        membershipCache.cacheMember(channelId, userId);
    }

    /** INVITE_ONLY 채널 초대. OWNER만 가능. 이미 멤버면 ALREADY_MEMBER. */
    @Transactional
    public void inviteMember(UUID channelId, UUID inviterId, UUID inviteeId) {
        getChannelOrThrow(channelId);
        requireOwner(channelId, inviterId);
        requireNotAlreadyMember(channelId, inviteeId);
        memberRepository.save(new ChannelMember(channelId, inviteeId, ChannelRole.MEMBER));
        membershipCache.cacheMember(channelId, inviteeId);
    }

    /**
     * 멤버 제외 또는 본인 탈퇴. OWNER가 나가면 채널을 ARCHIVED로 전환한다 (business-rules.md Q3 답변 B).
     */
    @Transactional
    public void removeMember(UUID channelId, UUID actingUserId, UUID targetUserId) {
        Channel channel = getChannelOrThrow(channelId);
        boolean isSelfLeaving = actingUserId.equals(targetUserId);
        if (!isSelfLeaving) {
            requireOwner(channelId, actingUserId);
        }

        boolean targetIsOwner = channel.getOwnerId().equals(targetUserId);
        memberRepository.deleteById(new ChannelMemberId(channelId, targetUserId));
        membershipCache.invalidate(channelId, targetUserId);

        if (targetIsOwner) {
            channel.archive();
            channelRepository.save(channel);
        }
    }

    /**
     * 멤버십 확인. 캐시를 먼저 보고, 캐시 미스면 DB로 진실을 확인한다 (Invariant: 캐시-DB 일관성).
     */
    @Transactional(readOnly = true)
    public boolean isMember(UUID channelId, UUID userId) {
        if (membershipCache.isCachedMember(channelId, userId)) {
            return true;
        }
        boolean isMember = memberRepository.findByChannelId(channelId).stream()
                .anyMatch(m -> m.getUserId().equals(userId));
        if (isMember) {
            membershipCache.cacheMember(channelId, userId);
        }
        return isMember;
    }

    public void requireMember(UUID channelId, UUID userId) {
        if (!isMember(channelId, userId)) {
            throw new NotAMemberException();
        }
    }

    @Transactional(readOnly = true)
    public List<Channel> listChannelsForUser(UUID userId) {
        return channelRepository.findAllById(
                memberRepository.findByUserId(userId).stream().map(ChannelMember::getChannelId).toList());
    }

    @Transactional(readOnly = true)
    public Channel getChannelOrThrow(UUID channelId) {
        return channelRepository.findById(channelId).orElseThrow(ChannelNotFoundException::new);
    }

    private void requireNotAlreadyMember(UUID channelId, UUID userId) {
        if (isMember(channelId, userId)) {
            throw new AlreadyMemberException();
        }
    }

    private void requireOwner(UUID channelId, UUID userId) {
        boolean isOwner = memberRepository.findByChannelId(channelId).stream()
                .anyMatch(m -> m.getUserId().equals(userId) && m.getRole() == ChannelRole.OWNER);
        if (!isOwner) {
            throw new ForbiddenActionException();
        }
    }
}
