package com.bubbletea.user.presentation.member;

import com.bubbletea.user.application.member.MemberCommandService;
import com.bubbletea.user.application.member.MemberQueryService;
import com.bubbletea.user.application.member.result.MemberInfoResult;
import com.bubbletea.user.presentation.member.dto.MemberInfoResponseDto;
import com.bubbletea.user.presentation.member.dto.MemberProfileUpdateRequestDto;
import com.bubbletea.user.presentation.member.support.MemberAccessValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock
    private MemberQueryService memberQueryService;

    @Mock
    private MemberCommandService memberCommandService;

    private MemberController memberController;

    @BeforeEach
    void setUp() {
        memberController =
                new MemberController(
                        memberQueryService,
                        memberCommandService,
                        new MemberAccessValidator()
                );
    }

    @Test
    @DisplayName("회원은 자신의 상세 정보를 조회할 수 있다")
    void ownerCanReadOwnDetail() {
        // given
        MemberInfoResult result =
                new MemberInfoResult(
                        1L,
                        "member@example.com",
                        "테스터",
                        null,
                        "USER",
                        "ACTIVE"
                );

        when(memberQueryService.getById(1L))
                .thenReturn(result);

        // when
        MemberInfoResponseDto response =
                memberController.detail(
                        1L,
                        1L,
                        "USER"
                );

        // then
        assertEquals(
                1L,
                response.memberId()
        );

        assertEquals(
                "member@example.com",
                response.email()
        );

        verify(memberQueryService)
                .getById(1L);
    }

    @Test
    @DisplayName("회원이 다른 회원의 상세 정보를 조회하면 403을 반환한다")
    void userCannotReadAnotherMemberDetail() {
        // when
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> memberController.detail(
                                2L,
                                1L,
                                "USER"
                        )
                );

        // then
        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verify(
                memberQueryService,
                never()
        ).getById(2L);
    }

    @Test
    @DisplayName("관리자는 다른 회원의 상세 정보를 조회할 수 있다")
    void adminCanReadAnotherMemberDetail() {
        // given
        MemberInfoResult result =
                new MemberInfoResult(
                        2L,
                        "target@example.com",
                        "대상회원",
                        null,
                        "USER",
                        "ACTIVE"
                );

        when(memberQueryService.getById(2L))
                .thenReturn(result);

        // when
        MemberInfoResponseDto response =
                memberController.detail(
                        2L,
                        99L,
                        "ADMIN"
                );

        // then
        assertEquals(
                2L,
                response.memberId()
        );

        verify(memberQueryService)
                .getById(2L);
    }

    @Test
    @DisplayName("회원은 자신의 프로필을 수정할 수 있다")
    void ownerCanUpdateOwnProfile() {
        // given
        MemberProfileUpdateRequestDto request =
                new MemberProfileUpdateRequestDto(
                        "새닉네임",
                        "https://example.com/profile.png"
                );

        // when
        memberController.edit(
                1L,
                1L,
                "USER",
                request
        );

        // then
        verify(memberCommandService)
                .update(
                        1L,
                        request.toCommand()
                );
    }

    @Test
    @DisplayName("회원이 다른 회원의 프로필을 수정하면 403을 반환한다")
    void userCannotUpdateAnotherMemberProfile() {
        // given
        MemberProfileUpdateRequestDto request =
                new MemberProfileUpdateRequestDto(
                        "위조닉네임",
                        null
                );

        // when
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> memberController.edit(
                                2L,
                                1L,
                                "USER",
                                request
                        )
                );

        // then
        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verify(
                memberCommandService,
                never()
        ).update(
                2L,
                request.toCommand()
        );
    }

    @Test
    @DisplayName("회원은 자신의 계정을 탈퇴할 수 있다")
    void ownerCanDeleteOwnAccount() {
        // when
        memberController.delete(
                1L,
                1L,
                "USER"
        );

        // then
        verify(memberCommandService)
                .delete(1L);
    }

    @Test
    @DisplayName("회원이 다른 회원을 탈퇴시키면 403을 반환한다")
    void userCannotDeleteAnotherMember() {
        // when
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> memberController.delete(
                                2L,
                                1L,
                                "USER"
                        )
                );

        // then
        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verify(
                memberCommandService,
                never()
        ).delete(2L);
    }
}