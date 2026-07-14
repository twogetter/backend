package com.bubbletea.auth.presentation.auth.dto;

import com.bubbletea.auth.application.auth.result.SignUpResult;

public record SignUpResponse(
        Long memberId,
        String email,
        String nickname
) {

    public static SignUpResponse from(SignUpResult result) {
        return new SignUpResponse(
                result.memberId(),
                result.email(),
                result.nickname()
        );
    }
}