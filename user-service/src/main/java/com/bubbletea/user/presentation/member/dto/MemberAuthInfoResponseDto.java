package com.bubbletea.user.presentation.member.dto;

import com.bubbletea.user.application.member.result.MemberAuthInfoResult;

public record MemberAuthInfoResponseDto(
        Long memberId,
        String email,
        String nickname,
        String role,
        String status,
        boolean loginAvailable
) {

    public static MemberAuthInfoResponseDto from(
            MemberAuthInfoResult result
    ) {
        return new MemberAuthInfoResponseDto(
                result.memberId(),
                result.email(),
                result.nickname(),
                result.role(),
                result.status(),
                result.loginAvailable()
        );
    }
}