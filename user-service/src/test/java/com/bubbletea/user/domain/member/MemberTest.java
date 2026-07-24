package com.bubbletea.user.domain.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    @DisplayName("일반 회원을 생성하면 USER 역할과 ACTIVE 상태로 생성된다")
    void createUserSuccess() {
        // when
        Member member =
                Member.createUser(
                        "member@example.com",
                        "회원"
                );

        // then
        assertThat(member.getEmail())
                .isEqualTo("member@example.com");

        assertThat(member.getNickname())
                .isEqualTo("회원");

        assertThat(member.getRole())
                .isEqualTo(MemberRole.USER);

        assertThat(member.getStatus())
                .isEqualTo(MemberStatus.ACTIVE);

        assertThat(member.getCreatedAt())
                .isNotNull();

        assertThat(member.getWithdrawnAt())
                .isNull();
    }

    @Test
    @DisplayName("닉네임과 프로필 이미지를 함께 수정한다")
    void updateProfileSuccess() {
        // given
        Member member =
                Member.createUser(
                        "member@example.com",
                        "기존닉네임"
                );

        // when
        member.updateProfile(
                "변경닉네임",
                "https://example.com/profile.png"
        );

        // then
        assertThat(member.getNickname())
                .isEqualTo("변경닉네임");

        assertThat(member.getProfileImageUrl())
                .isEqualTo(
                        "https://example.com/profile.png"
                );
    }

    @Test
    @DisplayName("닉네임이 null이면 기존 닉네임을 유지한다")
    void updateProfileKeepsNicknameWhenNicknameIsNull() {
        // given
        Member member =
                Member.createUser(
                        "member@example.com",
                        "기존닉네임"
                );

        // when
        member.updateProfile(
                null,
                "https://example.com/profile.png"
        );

        // then
        assertThat(member.getNickname())
                .isEqualTo("기존닉네임");

        assertThat(member.getProfileImageUrl())
                .isEqualTo(
                        "https://example.com/profile.png"
                );
    }

    @Test
    @DisplayName("닉네임이 공백이면 기존 닉네임을 유지한다")
    void updateProfileKeepsNicknameWhenNicknameIsBlank() {
        // given
        Member member =
                Member.createUser(
                        "member@example.com",
                        "기존닉네임"
                );

        // when
        member.updateProfile(
                "   ",
                null
        );

        // then
        assertThat(member.getNickname())
                .isEqualTo("기존닉네임");

        assertThat(member.getProfileImageUrl())
                .isNull();
    }

    @Test
    @DisplayName("프로필 이미지가 null이면 기존 프로필 이미지를 유지한다")
    void updateProfileKeepsProfileImageWhenImageUrlIsNull() {
        // given
        Member member =
                Member.createUser(
                        "member@example.com",
                        "기존닉네임"
                );

        member.updateProfile(
                null,
                "https://example.com/old.png"
        );

        // when
        member.updateProfile(
                "새닉네임",
                null
        );

        // then
        assertThat(member.getNickname())
                .isEqualTo("새닉네임");

        assertThat(member.getProfileImageUrl())
                .isEqualTo(
                        "https://example.com/old.png"
                );
    }

    @Test
    @DisplayName("활성 회원이 탈퇴하면 상태와 탈퇴 시간이 변경된다")
    void withdrawSuccess() {
        // given
        Member member =
                Member.createUser(
                        "member@example.com",
                        "회원"
                );

        // when
        member.withdraw();

        // then
        assertThat(member.getStatus())
                .isEqualTo(MemberStatus.WITHDRAWN);

        assertThat(member.getWithdrawnAt())
                .isNotNull();
    }

    @Test
    @DisplayName("이미 탈퇴한 회원이 다시 탈퇴하면 실패한다")
    void withdrawFailsWhenAlreadyWithdrawn() {
        // given
        Member member =
                Member.createUser(
                        "member@example.com",
                        "회원"
                );

        member.withdraw();

        // when & then
        assertThatThrownBy(member::withdraw)
                .isInstanceOf(
                        IllegalStateException.class
                );
    }

    @Test
    @DisplayName("활성 회원은 상태 검증을 통과한다")
    void validateActiveSuccess() {
        // given
        Member member =
                Member.createUser(
                        "member@example.com",
                        "회원"
                );

        // when & then
        assertThatCode(member::validateActive)
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("탈퇴 회원은 활성 상태 검증에 실패한다")
    void validateActiveFailsWhenWithdrawn() {
        // given
        Member member =
                Member.createUser(
                        "member@example.com",
                        "회원"
                );

        member.withdraw();

        // when & then
        assertThatThrownBy(member::validateActive)
                .isInstanceOf(
                        IllegalStateException.class
                );
    }

    @Test
    @DisplayName("정지 회원은 활성 상태 검증에 실패한다")
    void validateActiveFailsWhenSuspended() {
        // given
        Member member =
                Member.createUser(
                        "member@example.com",
                        "회원"
                );

        ReflectionTestUtils.setField(
                member,
                "status",
                MemberStatus.SUSPENDED
        );

        // when & then
        assertThatThrownBy(member::validateActive)
                .isInstanceOf(
                        IllegalStateException.class
                );
    }
}