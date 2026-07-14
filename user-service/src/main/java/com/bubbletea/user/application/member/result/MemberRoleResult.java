package com.bubbletea.user.application.member.result;

import com.bubbletea.user.domain.member.Member;

public record MemberRoleResult(
        Long memberId,
        String role,
        String status
) {
    public static MemberRoleResult from(Member member) {
        return new MemberRoleResult(
                member.getId(),
                member.getRole().name(),
                member.getStatus().name()
        );
    }
}