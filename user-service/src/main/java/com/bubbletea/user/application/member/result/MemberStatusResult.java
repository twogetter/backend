package com.bubbletea.user.application.member.result;

import com.bubbletea.user.domain.member.Member;

public record MemberStatusResult(
        Long memberId,
        String status,
        String role
) {
    public static MemberStatusResult from(Member member) {
        return new MemberStatusResult(
                member.getId(),
                member.getStatus().name(),
                member.getRole().name()
        );
    }
}