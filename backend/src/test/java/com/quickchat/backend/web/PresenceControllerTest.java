package com.quickchat.backend.web;

import com.quickchat.backend.security.JwtTokenProvider;
import com.quickchat.backend.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PresenceController의 REST 계약 테스트 (Gap 2 보완).
 * 보안 필터는 AuthControllerTest와 동일하게 이 단위 테스트 범위에서 제외한다.
 */
@WebMvcTest(controllers = PresenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class PresenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PresenceService presenceService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void returnsOnlineStatusForEachRequestedUserId() throws Exception {
        UUID onlineUser = UUID.randomUUID();
        UUID offlineUser = UUID.randomUUID();
        when(presenceService.isOnline(onlineUser)).thenReturn(true);
        when(presenceService.isOnline(offlineUser)).thenReturn(false);

        mockMvc.perform(get("/api/presence")
                        .param("userIds", onlineUser.toString(), offlineUser.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(onlineUser.toString()))
                .andExpect(jsonPath("$[0].online").value(true))
                .andExpect(jsonPath("$[1].userId").value(offlineUser.toString()))
                .andExpect(jsonPath("$[1].online").value(false));
    }

    @Test
    void returnsSingleEntryWhenOneUserIdRequested() throws Exception {
        UUID userId = UUID.randomUUID();
        when(presenceService.isOnline(userId)).thenReturn(true);

        mockMvc.perform(get("/api/presence").param("userIds", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].online").value(true));
    }
}
