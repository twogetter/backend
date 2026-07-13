package com.bubbletea.auth.application.auth.result;

public record TokenResult(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn
) {
}