package com.bubbletea.user.presentation.member.dto;

import com.bubbletea.user.application.member.command.UpdateProfileCommand;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequestDto(
        @Size(max = 30)
        String nickname,

        String profileImageUrl
) {
    public UpdateProfileCommand toCommand() {
        return new UpdateProfileCommand(nickname, profileImageUrl);
    }
}