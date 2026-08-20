package com.quickchat.backend.repository;

import com.quickchat.backend.domain.Channel;
import com.quickchat.backend.domain.ChannelType;
import com.quickchat.backend.domain.ChannelVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    // story 1.3(공개 채널 목록에서 바로 참여) - Frontend Code Generation 중 발견된 누락 보완.
    List<Channel> findByTypeAndVisibility(ChannelType type, ChannelVisibility visibility);
}
