package com.bubbletea.auth.application.auth.result;

public record SignUpResult(
        Long memberId,
        String email,
        String nickname
) {
}