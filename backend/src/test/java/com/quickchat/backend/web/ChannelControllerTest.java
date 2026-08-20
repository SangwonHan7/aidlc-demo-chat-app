package com.quickchat.backend.web;

import com.quickchat.backend.domain.ChannelMember;
import com.quickchat.backend.domain.ChannelRole;
import com.quickchat.backend.service.ChannelService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChannelController의 신규 멤버 목록 조회(listMembers)에 대한 계약 테스트만 다룬다 - 나머지 기존
 * 엔드포인트는 api-layer-summary.md에 이미 명시된 대로 Build and Test 단계로 계속 미룬다.
 */
@WebMvcTest(controllers = ChannelController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChannelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChannelService channelService;

    @Test
    void listDiscoverableReturnsPublicChannelsRegardlessOfMembership() throws Exception {
        UUID channelId = UUID.randomUUID();
        var channel = new com.quickchat.backend.domain.Channel(
                "general", com.quickchat.backend.domain.ChannelType.GROUP,
                com.quickchat.backend.domain.ChannelVisibility.PUBLIC, UUID.randomUUID());
        when(channelService.listDiscoverablePublicChannels()).thenReturn(List.of(channel));

        mockMvc.perform(get("/api/channels/discoverable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("general"))
                .andExpect(jsonPath("$[0].visibility").value("PUBLIC"));
    }

    @Test
    void listMembersReturnsUserIdAndRoleForEachMember() throws Exception {
        UUID channelId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        when(channelService.listMembers(eq(channelId), any())).thenReturn(List.of(
                new ChannelMember(channelId, ownerId, ChannelRole.OWNER),
                new ChannelMember(channelId, memberId, ChannelRole.MEMBER)
        ));

        mockMvc.perform(get("/api/channels/{channelId}/members", channelId)
                        .principal(principal(ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(ownerId.toString()))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].userId").value(memberId.toString()))
                .andExpect(jsonPath("$[1].role").value("MEMBER"));
    }

    private static Principal principal(UUID userId) {
        return () -> userId.toString();
    }

    private static UUID eq(UUID value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
