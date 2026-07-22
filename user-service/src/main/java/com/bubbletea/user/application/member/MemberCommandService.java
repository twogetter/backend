package com.bubbletea.user.application.member;

import com.bubbletea.user.application.member.command.CreateMemberCommand;
import com.bubbletea.user.application.member.command.UpdateProfileCommand;
import com.bubbletea.user.application.member.result.MemberInfoResult;
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
     * Auth Service의 회원가입 요청을 받아
     * user-service에 회원 기본 정보를 생성한다.
     */
    @Transactional
    public MemberInfoResult save(
            CreateMemberCommand command
    ) {
        validateDuplicateEmail(command.email());
        validateDuplicateNickname(command.nickname());

        Member member = Member.createUser(
                command.email(),
                command.nickname()
        );

        Member savedMember =
                memberRepository.save(member);

        return MemberInfoResult.from(savedMember);
    }

    @Transactional
    public void update(
            Long memberId,
            UpdateProfileCommand command
    ) {
        Member member =
                getMemberByIdOrThrow(memberId);

        member.validateActive();

        if (
                command.nickname() != null
                        && !command.nickname().isBlank()
        ) {
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
     * 일반 회원 탈퇴는 데이터를 삭제하지 않고
     * 회원 상태를 WITHDRAWN으로 변경한다.
     */
    @Transactional
    public void delete(Long memberId) {
        Member member =
                getMemberByIdOrThrow(memberId);

        member.validateActive();
        member.withdraw();
    }

    /**
     * 회원가입 과정에서 Auth 계정 저장이 실패했을 때 호출한다.
     *
     * 회원가입 자체가 완료되지 않은 상태이므로
     * user-service에 먼저 저장된 회원을 실제 삭제한다.
     */
    @Transactional
    public void rollbackSignUp(Long memberId) {
        Member member =
                getMemberByIdOrThrow(memberId);

        memberRepository.delete(member);
    }

    private void validateDuplicateEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 이메일입니다."
            );
        }
    }

    private void validateDuplicateNickname(
            String nickname
    ) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException(
                    "이미 사용 중인 닉네임입니다."
            );
        }
    }

    private Member getMemberByIdOrThrow(
            Long memberId
    ) {
        return memberRepository.findById(memberId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원을 찾을 수 없습니다. memberId="
                                        + memberId
                        )
                );
    }
}