package com.bubbletea.auth.application.auth.command;

public record RefreshTokenCommand(
        String refreshToken
) {
}