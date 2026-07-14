package com.bubbletea.user.presentation.member.dto;

import com.bubbletea.user.application.member.result.MemberStatusResult;

public record MemberStatusResponseDto(
        Long memberId,
        String status,
        String role
) {
    public static MemberStatusResponseDto from(MemberStatusResult result) {
        return new MemberStatusResponseDto(
                result.memberId(),
                result.status(),
                result.role()
        );
    }
}