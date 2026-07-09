package com.bubbletea.user.presentation.member.dto;

import com.bubbletea.user.application.member.result.MemberProfileResult;

public record MemberProfileResponseDto(
        Long memberId,
        String nickname,
        String profileImageUrl,
        String status
) {
    public static MemberProfileResponseDto from(MemberProfileResult result) {
        return new MemberProfileResponseDto(
                result.memberId(),
                result.nickname(),
                result.profileImageUrl(),
                result.status()
        );
    }
}