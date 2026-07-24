package com.bubbletea.user.application.member;

import com.bubbletea.user.application.member.result.MemberInfoResult;
import com.bubbletea.user.application.member.result.MemberProfileResult;
import com.bubbletea.user.application.member.result.MemberRoleResult;
import com.bubbletea.user.application.member.result.MemberStatusResult;
import com.bubbletea.user.domain.member.Member;
import com.bubbletea.user.domain.member.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceAdditionalTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberQueryService memberQueryService;

    @Test
    @DisplayName("활성 회원의 상세 정보를 조회한다")
    void getByIdSuccess() {
        Member member = createMember(1L);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        MemberInfoResult result =
                memberQueryService.getById(1L);

        assertThat(result.memberId())
                .isEqualTo(1L);

        assertThat(result.email())
                .isEqualTo("member@example.com");

        assertThat(result.nickname())
                .isEqualTo("회원");

        assertThat(result.role())
                .isEqualTo("USER");

        assertThat(result.status())
                .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("탈퇴 회원의 상세 정보를 조회하면 실패한다")
    void getByIdFailsWhenMemberIsWithdrawn() {
        Member member = createMember(1L);
        member.withdraw();

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(
                () -> memberQueryService.getById(1L)
        )
                .isInstanceOf(
                        IllegalStateException.class
                );
    }

    @Test
    @DisplayName("회원 상태 정보를 조회한다")
    void getStatusSuccess() {
        Member member = createMember(1L);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        MemberStatusResult result =
                memberQueryService.getStatus(1L);

        assertThat(result.memberId())
                .isEqualTo(1L);

        assertThat(result.status())
                .isEqualTo("ACTIVE");

        assertThat(result.role())
                .isEqualTo("USER");
    }

    @Test
    @DisplayName("회원 프로필 정보를 조회한다")
    void getProfileSuccess() {
        Member member = createMember(1L);

        member.updateProfile(
                "변경회원",
                "https://example.com/profile.png"
        );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        MemberProfileResult result =
                memberQueryService.getProfile(1L);

        assertThat(result.memberId())
                .isEqualTo(1L);

        assertThat(result.nickname())
                .isEqualTo("변경회원");

        assertThat(result.profileImageUrl())
                .isEqualTo(
                        "https://example.com/profile.png"
                );

        assertThat(result.status())
                .isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("회원 역할 정보를 조회한다")
    void getRoleSuccess() {
        Member member = createMember(1L);

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        MemberRoleResult result =
                memberQueryService.getRole(1L);

        assertThat(result.memberId())
                .isEqualTo(1L);

        assertThat(result.role())
                .isEqualTo("USER");

        assertThat(result.status())
                .isEqualTo("ACTIVE");
    }

    private Member createMember(Long memberId) {
        Member member =
                Member.createUser(
                        "member@example.com",
                        "회원"
                );

        ReflectionTestUtils.setField(
                member,
                "id",
                memberId
        );

        return member;
    }
}