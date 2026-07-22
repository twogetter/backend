package com.bubbletea.user.application.member.result;

import com.bubbletea.user.domain.member.Member;
import com.bubbletea.user.domain.member.MemberStatus;

public record MemberAuthInfoResult(
        Long memberId,
        String email,
        String nickname,
        String role,
        String status,
        boolean loginAvailable
) {

    public static MemberAuthInfoResult from(
            Member member
    ) {
        return new MemberAuthInfoResult(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name(),
                member.getStatus().name(),
                member.getStatus() == MemberStatus.ACTIVE
        );
    }
}