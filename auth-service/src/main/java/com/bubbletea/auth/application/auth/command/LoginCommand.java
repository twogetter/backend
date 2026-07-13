package com.bubbletea.auth.application.auth.command;

public record LoginCommand(
        String email,
        String password
) {
}