package com.bubbletea.user.presentation.member.dto;

import com.bubbletea.user.application.member.result.MemberInfoResult;

public record MemberInfoResponseDto(
        Long memberId,
        String email,
        String nickname,
        String profileImageUrl,
        String role,
        String status
) {
    public static MemberInfoResponseDto from(MemberInfoResult result) {
        return new MemberInfoResponseDto(
                result.memberId(),
                result.email(),
                result.nickname(),
                result.profileImageUrl(),
                result.role(),
                result.status()
        );
    }
}