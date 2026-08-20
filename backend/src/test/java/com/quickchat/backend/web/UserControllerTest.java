package com.quickchat.backend.web;

import com.quickchat.backend.domain.User;
import com.quickchat.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @Test
    void meReturnsProfileForAuthenticatedPrincipal() throws Exception {
        User user = new User("me@example.com", "hashed", "Me");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me").principal(principal(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.displayName").value("Me"));
    }

    @Test
    void meReturns404WhenPrincipalUserNoLongerExists() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me").principal(principal(missingId)))
                .andExpect(status().isNotFound());
    }

    private static Principal principal(UUID userId) {
        return () -> userId.toString();
    }

    @Test
    void returnsUserProfilesForRequestedIds() throws Exception {
        User user = new User("member@example.com", "hashed", "Member One");
        when(userRepository.findAllById(List.of(user.getId()))).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users").param("ids", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(user.getId().toString()))
                .andExpect(jsonPath("$[0].displayName").value("Member One"));
    }

    @Test
    void searchByEmailReturnsSingleMatchWhenFound() throws Exception {
        User user = new User("found@example.com", "hashed", "Found User");
        when(userRepository.findByEmail("found@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/search").param("email", "found@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("found@example.com"));
    }

    @Test
    void searchByEmailReturnsEmptyListWhenNotFound() throws Exception {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/search").param("email", "missing@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
