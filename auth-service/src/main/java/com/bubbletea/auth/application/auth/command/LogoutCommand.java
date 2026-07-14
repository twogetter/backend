package com.bubbletea.auth.application.auth.command;

public record LogoutCommand(
        String refreshToken
) {
}