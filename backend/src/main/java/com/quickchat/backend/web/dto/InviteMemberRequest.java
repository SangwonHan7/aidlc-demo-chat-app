package com.quickchat.backend.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InviteMemberRequest(@NotNull UUID inviteeId) {
}
