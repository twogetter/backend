package com.bubbletea.user.application.member;

import com.bubbletea.user.application.member.result.MemberAuthInfoResult;
import com.bubbletea.user.domain.member.Member;
import com.bubbletea.user.domain.member.MemberRepository;
import com.bubbletea.user.domain.member.MemberRole;
import com.bubbletea.user.domain.member.MemberStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberQueryService memberQueryService;

    @Test
    @DisplayName("활성 회원 인증 정보에는 닉네임과 로그인 가능 여부를 반환한다")
    void getAuthInfoSuccess() {
        // given
        Member member =
                org.mockito.Mockito.mock(Member.class);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(member.getId())
                .thenReturn(1L);

        when(member.getEmail())
                .thenReturn("test@example.com");

        when(member.getNickname())
                .thenReturn("테스터");

        when(member.getRole())
                .thenReturn(MemberRole.USER);

        when(member.getStatus())
                .thenReturn(MemberStatus.ACTIVE);

        // when
        MemberAuthInfoResult result =
                memberQueryService.getAuthInfo(1L);

        // then
        assertThat(result.memberId())
                .isEqualTo(1L);

        assertThat(result.email())
                .isEqualTo("test@example.com");

        assertThat(result.nickname())
                .isEqualTo("테스터");

        assertThat(result.role())
                .isEqualTo("USER");

        assertThat(result.status())
                .isEqualTo("ACTIVE");

        assertThat(result.loginAvailable())
                .isTrue();
    }

    @Test
    @DisplayName("정지 회원은 로그인 불가능 상태를 반환한다")
    void suspendedMemberIsNotLoginAvailable() {
        // given
        Member member =
                org.mockito.Mockito.mock(Member.class);

        when(memberRepository.findById(2L))
                .thenReturn(Optional.of(member));

        when(member.getId())
                .thenReturn(2L);

        when(member.getEmail())
                .thenReturn("suspended@example.com");

        when(member.getNickname())
                .thenReturn("정지회원");

        when(member.getRole())
                .thenReturn(MemberRole.USER);

        when(member.getStatus())
                .thenReturn(MemberStatus.SUSPENDED);

        // when
        MemberAuthInfoResult result =
                memberQueryService.getAuthInfo(2L);

        // then
        assertThat(result.status())
                .isEqualTo("SUSPENDED");

        assertThat(result.loginAvailable())
                .isFalse();
    }
}