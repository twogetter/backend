package com.bubbletea.auth.infrastructure.client.dto;

public record CreateMemberInternalRequest(
        String email,
        String nickname
) {
}