package com.bubbletea.auth.application.auth.command;

public record SignUpCommand(
        String email,
        String password,
        String nickname
) {
}