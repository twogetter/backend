package com.bubbletea.gateway.authentication.jwt;

public record JwtClaims(
        Long userId,
        String role,
        String nickname
) {
}