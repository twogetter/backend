package com.bubbletea.user.presentation.member.dto;

import com.bubbletea.user.application.member.result.MemberRoleResult;

public record MemberRoleResponseDto(
        Long memberId,
        String role,
        String status
) {
    public static MemberRoleResponseDto from(MemberRoleResult result) {
        return new MemberRoleResponseDto(
                result.memberId(),
                result.role(),
                result.status()
        );
    }
}