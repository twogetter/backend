package com.bubbletea.user.presentation.member.dto;

import com.bubbletea.user.application.member.result.MemberInfoResult;

public record MemberCreateResponseDto(
        Long memberId,
        String email,
        String nickname,
        String role,
        String status
) {

    public static MemberCreateResponseDto from(
            MemberInfoResult result
    ) {
        return new MemberCreateResponseDto(
                result.memberId(),
                result.email(),
                result.nickname(),
                result.role(),
                result.status()
        );
    }
}