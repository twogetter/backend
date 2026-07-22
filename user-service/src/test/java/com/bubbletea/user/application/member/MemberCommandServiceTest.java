package com.bubbletea.user.application.member;

import com.bubbletea.user.application.member.command.CreateMemberCommand;
import com.bubbletea.user.application.member.result.MemberInfoResult;
import com.bubbletea.user.domain.member.Member;
import com.bubbletea.user.domain.member.MemberRepository;
import com.bubbletea.user.domain.member.MemberRole;
import com.bubbletea.user.domain.member.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberCommandService memberCommandService;

    @Test
    @DisplayName("회원 생성 성공 시 생성된 회원 정보를 반환한다")
    void saveSuccess() {
        // given
        String email = "test@example.com";
        String nickname = "테스터";

        CreateMemberCommand command =
                new CreateMemberCommand(
                        email,
                        nickname
                );

        Member savedMember =
                org.mockito.Mockito.mock(Member.class);

        when(memberRepository.existsByEmail(email))
                .thenReturn(false);

        when(memberRepository.existsByNickname(nickname))
                .thenReturn(false);

        when(memberRepository.save(any(Member.class)))
                .thenReturn(savedMember);

        when(savedMember.getId())
                .thenReturn(1L);

        when(savedMember.getEmail())
                .thenReturn(email);

        when(savedMember.getNickname())
                .thenReturn(nickname);

        when(savedMember.getRole())
                .thenReturn(MemberRole.USER);

        when(savedMember.getStatus())
                .thenReturn(MemberStatus.ACTIVE);

        // when
        MemberInfoResult result =
                memberCommandService.save(command);

        // then
        assertThat(result.memberId())
                .isEqualTo(1L);

        assertThat(result.email())
                .isEqualTo(email);

        assertThat(result.nickname())
                .isEqualTo(nickname);

        assertThat(result.role())
                .isEqualTo("USER");

        assertThat(result.status())
                .isEqualTo("ACTIVE");

        ArgumentCaptor<Member> memberCaptor =
                ArgumentCaptor.forClass(Member.class);

        verify(memberRepository)
                .save(memberCaptor.capture());

        Member createdMember =
                memberCaptor.getValue();

        assertThat(createdMember.getEmail())
                .isEqualTo(email);

        assertThat(createdMember.getNickname())
                .isEqualTo(nickname);
    }

    @Test
    @DisplayName("중복 이메일이면 회원 생성에 실패한다")
    void saveFailsWhenEmailIsDuplicated() {
        // given
        String email = "duplicate@example.com";

        CreateMemberCommand command =
                new CreateMemberCommand(
                        email,
                        "테스터"
                );

        when(memberRepository.existsByEmail(email))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(
                () -> memberCommandService.save(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        verify(memberRepository, never())
                .save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 롤백 시 생성된 회원을 실제 삭제한다")
    void rollbackSignUpSuccess() {
        // given
        Member member =
                org.mockito.Mockito.mock(Member.class);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        // when
        memberCommandService.rollbackSignUp(1L);

        // then
        verify(memberRepository)
                .delete(member);
    }
}