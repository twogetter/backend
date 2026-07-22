package com.bubbletea.user.presentation.member.dto;

import com.bubbletea.user.application.member.command.CreateMemberCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberCreateRequestDto(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(max = 30)
        String nickname
) {
    public CreateMemberCommand toCommand() {
        return new CreateMemberCommand(email, nickname);
    }
}