package com.bubbletea.user.application.member.result;

import com.bubbletea.user.domain.member.Member;

public record MemberProfileResult(
        Long memberId,
        String nickname,
        String profileImageUrl,
        String status
) {
    public static MemberProfileResult from(Member member) {
        return new MemberProfileResult(
                member.getId(),
                member.getNickname(),
                member.getProfileImageUrl(),
                member.getStatus().name()
        );
    }
}