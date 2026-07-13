package com.bubbletea.auth.presentation.auth.dto;

import com.bubbletea.auth.application.auth.result.TokenResult;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    private static final String BEARER = "Bearer";

    public static TokenResponse from(TokenResult result) {
        return new TokenResponse(
                result.accessToken(),
                BEARER,
                result.accessTokenExpiresIn()
        );
    }
}