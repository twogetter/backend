package com.bubbletea.user.application.member.command;

public record UpdateProfileCommand(
        String nickname,
        String profileImageUrl
) {
}