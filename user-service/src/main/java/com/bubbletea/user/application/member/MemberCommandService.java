package com.bubbletea.user.application.member;

import com.bubbletea.user.application.member.command.CreateMemberCommand;
import com.bubbletea.user.application.member.command.UpdateProfileCommand;
import com.bubbletea.user.domain.member.Member;
import com.bubbletea.user.domain.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCommandService {

    private final MemberRepository memberRepository;

    @Transactional
    public Long save(CreateMemberCommand command) {
        if (memberRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        if (memberRepository.existsByNickname(command.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        Member member = Member.createUser(command.email(), command.nickname());

        return memberRepository.save(member).getId();
    }

    @Transactional
    public void update(Long memberId, UpdateProfileCommand command) {
        Member member = getMemberByIdOrThrow(memberId);

        member.validateActive();

        if (command.nickname() != null && !command.nickname().isBlank()) {
            boolean duplicated = memberRepository.existsByNicknameAndIdNot(
                    command.nickname(),
                    memberId
            );

            if (duplicated) {
                throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
            }
        }

        member.updateProfile(command.nickname(), command.profileImageUrl());
    }

    @Transactional
    public void delete(Long memberId) {
        Member member = getMemberByIdOrThrow(memberId);
        member.validateActive();
        member.withdraw();
    }

    private Member getMemberByIdOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }
}