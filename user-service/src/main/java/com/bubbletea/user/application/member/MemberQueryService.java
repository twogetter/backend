package com.bubbletea.user.application.member;

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

    public MemberStatusResult getStatus(Long memberId) {
        Member member = getMember(memberId);

        return MemberStatusResult.from(member);
    }

    public MemberProfileResult getProfile(Long memberId) {
        Member member = getMember(memberId);

        return MemberProfileResult.from(member);
    }

    public MemberRoleResult getRole(Long memberId) {
        Member member = getMember(memberId);

        return MemberRoleResult.from(member);
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다. memberId=" + memberId));
    }
}