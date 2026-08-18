package com.quickchat.backend.web.dto;

import com.quickchat.backend.domain.ChannelVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 그룹 채널 생성 요청. DIRECT 채널은 별도 API(startDirectMessage)로 생성되어 여기서 다루지 않는다. */
public record CreateChannelRequest(
        @NotBlank String name,
        @NotNull ChannelVisibility visibility
) {
}
