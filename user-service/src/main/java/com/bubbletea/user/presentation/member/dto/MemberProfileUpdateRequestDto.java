package com.bubbletea.user.presentation.member.dto;

import com.bubbletea.user.application.member.command.UpdateProfileCommand;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberProfileUpdateRequestDto(

        @Size(
                max = 30,
                message = "닉네임은 30자 이하여야 합니다."
        )
        @Pattern(
                regexp = ".*\\S.*",
                message = "닉네임은 공백으로만 구성할 수 없습니다."
        )
        String nickname,

        String profileImageUrl
) {

    public UpdateProfileCommand toCommand() {
        return new UpdateProfileCommand(
                nickname,
                profileImageUrl
        );
    }
}