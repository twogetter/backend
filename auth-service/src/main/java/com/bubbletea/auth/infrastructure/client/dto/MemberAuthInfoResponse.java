package com.bubbletea.auth.infrastructure.client.dto;

public record MemberAuthInfoResponse(
        Long memberId,
        String email,
        String role,
        String status,
        boolean loginAvailable
) {
}