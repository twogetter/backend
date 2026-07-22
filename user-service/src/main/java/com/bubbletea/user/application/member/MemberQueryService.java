package com.bubbletea.user.application.member;

import com.bubbletea.user.application.member.result.MemberAuthInfoResult;
import com.bubbletea.user.application.member.result.MemberInfoResult;
import com.bubbletea.user.application.member.result.MemberProfileResult;
import com.bubbletea.user.application.member.result.MemberRoleResult;
import com.bubbletea.user.application.member.result.MemberStatusResult;
import com.bubbletea.user.domain.member.Member;
import com.bubbletea.user.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;

    public MemberInfoResult getById(Long memberId) {
        Member member = getMember(memberId);
        member.validateActive();

        return MemberInfoResult.from(member);
    }

    /**
     * Auth Service 로그인 및 토큰 재발급에 사용된다.
     *
     * 닉네임, 역할, 회원 상태와 함께
     * 현재 로그인 가능한 회원인지 반환한다.
     */
    public MemberAuthInfoResult getAuthInfo(
            Long memberId
    ) {
        Member member = getMember(memberId);

        return MemberAuthInfoResult.from(member);
    }

    public MemberStatusResult getStatus(
            Long memberId
    ) {
        Member member = getMember(memberId);

        return MemberStatusResult.from(member);
    }

    public MemberProfileResult getProfile(
            Long memberId
    ) {
        Member member = getMember(memberId);

        return MemberProfileResult.from(member);
    }

    public MemberRoleResult getRole(
            Long memberId
    ) {
        Member member = getMember(memberId);

        return MemberRoleResult.from(member);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "회원을 찾을 수 없습니다. memberId="
                                        + memberId
                        )
                );
    }
}