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

    /**
     * 회원 생성
     */
    @Transactional
    public Long save(CreateMemberCommand command) {
        if (memberRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }

        if (memberRepository.existsByNickname(command.nickname())) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 닉네임입니다."
            );
        }

        Member member = Member.createUser(
                command.email(),
                command.nickname()
        );

        return memberRepository.save(member).getId();
    }

    /**
     * 회원 프로필 수정
     */
    @Transactional
    public void update(
            Long memberId,
            UpdateProfileCommand command
    ) {
        Member member = getMemberByIdOrThrow(memberId);

        member.validateActive();

        if (command.nickname() != null
                && !command.nickname().isBlank()) {

            boolean duplicated =
                    memberRepository.existsByNicknameAndIdNot(
                            command.nickname(),
                            memberId
                    );

            if (duplicated) {
                throw new IllegalArgumentException(
                        "이미 사용 중인 닉네임입니다."
                );
            }
        }

        member.updateProfile(
                command.nickname(),
                command.profileImageUrl()
        );
    }

    /**
     * 일반 회원 탈퇴
     *
     * 실제 삭제가 아니라 WITHDRAWN 상태로 변경한다.
     */
    @Transactional
    public void delete(Long memberId) {
        Member member = getMemberByIdOrThrow(memberId);

        member.validateActive();
        member.withdraw();
    }

    /**
     * 회원가입 보상 처리
     *
     * Auth 계정 저장 실패 시 user-service에 먼저 생성된
     * 회원 데이터를 실제 삭제한다.
     *
     * 회원이 이미 존재하지 않아도 예외 없이 성공하도록
     * 멱등하게 처리한다.
     */
    @Transactional
    public void rollbackSignUp(Long memberId) {
        memberRepository.findById(memberId)
                .ifPresent(memberRepository::delete);
    }

    private Member getMemberByIdOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원을 찾을 수 없습니다."
                        )
                );
    }
}