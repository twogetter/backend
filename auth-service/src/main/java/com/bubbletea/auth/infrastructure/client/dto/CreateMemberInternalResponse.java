package com.bubbletea.auth.infrastructure.client.dto;

public record CreateMemberInternalResponse(
        Long memberId,
        String email,
        String nickname,
        String role,
        String status
) {
}