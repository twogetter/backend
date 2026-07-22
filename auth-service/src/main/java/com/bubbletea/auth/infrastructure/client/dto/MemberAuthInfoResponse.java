package com.bubbletea.auth.infrastructure.client.dto;

public record MemberAuthInfoResponse(
        Long memberId,
        String email,
        String nickname,
        String role,
        String status,
        boolean loginAvailable
) {
}