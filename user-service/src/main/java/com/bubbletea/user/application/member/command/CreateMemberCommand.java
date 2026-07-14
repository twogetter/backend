package com.bubbletea.user.application.member.command;

public record CreateMemberCommand(
        String email,
        String nickname
) {
}