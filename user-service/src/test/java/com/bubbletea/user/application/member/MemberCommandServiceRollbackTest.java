package com.bubbletea.user.application.member;

import com.bubbletea.user.domain.member.Member;
import com.bubbletea.user.domain.member.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceRollbackTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberCommandService memberCommandService;

    @Test
    @DisplayName("회원가입 보상 처리 시 생성된 회원을 실제 삭제한다")
    void rollbackSignUpDeletesCreatedMember() {
        // given
        Long memberId = 10L;
        Member member = org.mockito.Mockito.mock(Member.class);

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        // when
        memberCommandService.rollbackSignUp(memberId);

        // then
        verify(memberRepository)
                .findById(memberId);

        verify(memberRepository)
                .delete(member);
    }

    @Test
    @DisplayName("이미 삭제된 회원의 보상 요청도 예외 없이 성공한다")
    void rollbackSignUpIsIdempotentWhenMemberDoesNotExist() {
        // given
        Long memberId = 10L;

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.empty());

        // when
        memberCommandService.rollbackSignUp(memberId);

        // then
        verify(memberRepository)
                .findById(memberId);

        verify(memberRepository, never())
                .delete(any(Member.class));
    }
}