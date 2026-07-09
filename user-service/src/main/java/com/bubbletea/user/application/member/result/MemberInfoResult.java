package com.bubbletea.user.application.member.result;

import com.bubbletea.user.domain.member.Member;

public record MemberInfoResult(
        Long memberId,
        String email,
        String nickname,
        String profileImageUrl,
        String role,
        String status
) {
    public static MemberInfoResult from(Member member) {
        return new MemberInfoResult(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getRole().name(),
                member.getStatus().name()
        );
    }
}