package com.quickchat.backend.repository;

import com.quickchat.backend.domain.ChannelMember;
import com.quickchat.backend.domain.ChannelMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, ChannelMemberId> {

    List<ChannelMember> findByChannelId(UUID channelId);

    List<ChannelMember> findByUserId(UUID userId);

    long countByChannelId(UUID channelId);
}
