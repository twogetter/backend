package com.bubbletea.user.application.member;

import com.bubbletea.user.application.member.command.UpdateProfileCommand;
import com.bubbletea.user.domain.member.Member;
import com.bubbletea.user.domain.member.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class MemberCommandServiceAdditionalTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberCommandService memberCommandService;

    @Test
    @DisplayName("활성 회원은 닉네임과 프로필 이미지를 수정할 수 있다")
    void updateProfileSuccess() {
        Member member =
                Member.createUser(
                        "member@example.com",
                        "기존닉네임"
                );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(
                memberRepository.existsByNicknameAndIdNot(
                        "변경닉네임",
                        1L
                )
        ).thenReturn(false);

        memberCommandService.update(
                1L,
                new UpdateProfileCommand(
                        "변경닉네임",
                        "https://example.com/profile.png"
                )
        );

        assertThat(member.getNickname())
                .isEqualTo("변경닉네임");

        assertThat(member.getProfileImageUrl())
                .isEqualTo(
                        "https://example.com/profile.png"
                );
    }

    @Test
    @DisplayName("다른 회원이 사용하는 닉네임으로 수정하면 실패한다")
    void updateFailsWhenNicknameIsDuplicated() {
        Member member =
                Member.createUser(
                        "member@example.com",
                        "기존닉네임"
                );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        when(
                memberRepository.existsByNicknameAndIdNot(
                        "중복닉네임",
                        1L
                )
        ).thenReturn(true);

        assertThatThrownBy(
                () -> memberCommandService.update(
                        1L,
                        new UpdateProfileCommand(
                                "중복닉네임",
                                null
                        )
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                );

        assertThat(member.getNickname())
                .isEqualTo("기존닉네임");
    }

    @Test
    @DisplayName("닉네임이 null이면 중복 검사를 생략하고 프로필 이미지만 수정한다")
    void updateSkipsNicknameCheckWhenNicknameIsNull() {
        Member member =
                Member.createUser(
                        "member@example.com",
                        "기존닉네임"
                );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        memberCommandService.update(
                1L,
                new UpdateProfileCommand(
                        null,
                        "https://example.com/new.png"
                )
        );

        verify(
                memberRepository,
                never()
        ).existsByNicknameAndIdNot(
                any(),
                any()
        );

        assertThat(member.getNickname())
                .isEqualTo("기존닉네임");

        assertThat(member.getProfileImageUrl())
                .isEqualTo(
                        "https://example.com/new.png"
                );
    }

    @Test
    @DisplayName("닉네임이 공백이면 중복 검사를 생략한다")
    void updateSkipsNicknameCheckWhenNicknameIsBlank() {
        Member member =
                Member.createUser(
                        "member@example.com",
                        "기존닉네임"
                );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        memberCommandService.update(
                1L,
                new UpdateProfileCommand(
                        "   ",
                        null
                )
        );

        verify(
                memberRepository,
                never()
        ).existsByNicknameAndIdNot(
                any(),
                any()
        );

        assertThat(member.getNickname())
                .isEqualTo("기존닉네임");
    }

    @Test
    @DisplayName("존재하지 않는 회원의 프로필을 수정하면 실패한다")
    void updateFailsWhenMemberDoesNotExist() {
        when(memberRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> memberCommandService.update(
                        999L,
                        new UpdateProfileCommand(
                                "변경닉네임",
                                null
                        )
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                );
    }

    @Test
    @DisplayName("활성 회원을 탈퇴 처리한다")
    void deleteSuccess() {
        Member member =
                Member.createUser(
                        "member@example.com",
                        "회원"
                );

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        memberCommandService.delete(1L);

        assertThat(member.getStatus().name())
                .isEqualTo("WITHDRAWN");

        assertThat(member.getWithdrawnAt())
                .isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 회원을 탈퇴 처리하면 실패한다")
    void deleteFailsWhenMemberDoesNotExist() {
        when(memberRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> memberCommandService.delete(999L)
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                );
    }

    @Test
    @DisplayName("이미 탈퇴한 회원을 다시 탈퇴 처리하면 실패한다")
    void deleteFailsWhenMemberIsAlreadyWithdrawn() {
        Member member =
                Member.createUser(
                        "member@example.com",
                        "회원"
                );

        member.withdraw();

        when(memberRepository.findById(1L))
                .thenReturn(Optional.of(member));

        assertThatThrownBy(
                () -> memberCommandService.delete(1L)
        )
                .isInstanceOf(
                        IllegalStateException.class
                );
    }

    @Test
    @DisplayName("롤백 대상 회원이 없으면 삭제하지 않는다")
    void rollbackSignUpDoesNothingWhenMemberDoesNotExist() {
        when(memberRepository.findById(999L))
                .thenReturn(Optional.empty());

        memberCommandService.rollbackSignUp(999L);

        verify(
                memberRepository,
                never()
        ).delete(any(Member.class));
    }
}