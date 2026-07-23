package com.bubbletea.auth.application.auth;

import com.bubbletea.auth.application.auth.command.LoginCommand;
import com.bubbletea.auth.application.auth.command.LogoutCommand;
import com.bubbletea.auth.application.auth.command.RefreshTokenCommand;
import com.bubbletea.auth.application.auth.command.SignUpCommand;
import com.bubbletea.auth.application.auth.result.SignUpResult;
import com.bubbletea.auth.application.auth.result.TokenResult;
import com.bubbletea.auth.domain.auth.AuthAccount;
import com.bubbletea.auth.domain.auth.AuthAccountRepository;
import com.bubbletea.auth.domain.auth.RefreshTokenStore;
import com.bubbletea.auth.infrastructure.client.UserServiceClient;
import com.bubbletea.auth.infrastructure.client.dto.CreateMemberInternalRequest;
import com.bubbletea.auth.infrastructure.client.dto.CreateMemberInternalResponse;
import com.bubbletea.auth.infrastructure.client.dto.MemberAuthInfoResponse;
import com.bubbletea.auth.infrastructure.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

    @Mock
    private AuthAccountRepository authAccountRepository;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthCommandService authCommandService;

    @Test
    @DisplayName("회원가입 성공 시 비밀번호를 암호화하고 인증 계정을 저장한다")
    void signUpSuccess() {
        // given
        String email = "test@example.com";
        String password = "Password123!";
        String nickname = "테스터";
        String encodedPassword = "encoded-password";

        SignUpCommand command =
                new SignUpCommand(
                        email,
                        password,
                        nickname
                );

        CreateMemberInternalResponse memberResponse =
                new CreateMemberInternalResponse(
                        1L,
                        email,
                        nickname,
                        "USER",
                        "ACTIVE"
                );

        when(authAccountRepository.existsByEmail(email))
                .thenReturn(false);

        when(passwordEncoder.encode(password))
                .thenReturn(encodedPassword);

        when(userServiceClient.createMember(
                any(CreateMemberInternalRequest.class)
        )).thenReturn(memberResponse);

        when(authAccountRepository.save(
                any(AuthAccount.class)
        )).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        // when
        SignUpResult result =
                authCommandService.signUp(command);

        // then
        assertThat(result.memberId())
                .isEqualTo(1L);

        assertThat(result.email())
                .isEqualTo(email);

        assertThat(result.nickname())
                .isEqualTo(nickname);

        ArgumentCaptor<CreateMemberInternalRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        CreateMemberInternalRequest.class
                );

        verify(userServiceClient)
                .createMember(requestCaptor.capture());

        CreateMemberInternalRequest capturedRequest =
                requestCaptor.getValue();

        assertThat(capturedRequest.email())
                .isEqualTo(email);

        assertThat(capturedRequest.nickname())
                .isEqualTo(nickname);

        ArgumentCaptor<AuthAccount> accountCaptor =
                ArgumentCaptor.forClass(AuthAccount.class);

        verify(authAccountRepository)
                .save(accountCaptor.capture());

        AuthAccount capturedAccount =
                accountCaptor.getValue();

        assertThat(capturedAccount.getMemberId())
                .isEqualTo(1L);

        assertThat(capturedAccount.getEmail())
                .isEqualTo(email);

        assertThat(capturedAccount.getPasswordHash())
                .isEqualTo(encodedPassword);

        verify(passwordEncoder)
                .encode(password);

        verify(authAccountRepository)
                .existsByEmail(email);

        verify(userServiceClient, never())
                .rollbackSignUp(anyLong());
    }

    @Test
    @DisplayName("이미 사용 중인 이메일이면 회원가입에 실패한다")
    void signUpFailsWhenEmailAlreadyExists() {
        // given
        String email = "duplicate@example.com";

        SignUpCommand command =
                new SignUpCommand(
                        email,
                        "Password123!",
                        "중복테스터"
                );

        when(authAccountRepository.existsByEmail(email))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(
                () -> authCommandService.signUp(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        verify(passwordEncoder, never())
                .encode(any(CharSequence.class));

        verify(userServiceClient, never())
                .createMember(
                        any(CreateMemberInternalRequest.class)
                );

        verify(authAccountRepository, never())
                .save(any(AuthAccount.class));

        verify(userServiceClient, never())
                .rollbackSignUp(anyLong());
    }

    @Test
    @DisplayName("Auth 계정 저장 실패 시 user-service 회원 생성을 롤백한다")
    void signUpRollsBackMemberWhenAuthAccountSaveFails() {
        // given
        String email = "rollback@example.com";
        String password = "Password123!";
        String nickname = "롤백테스터";
        String encodedPassword = "encoded-password";

        SignUpCommand command =
                new SignUpCommand(
                        email,
                        password,
                        nickname
                );

        CreateMemberInternalResponse memberResponse =
                new CreateMemberInternalResponse(
                        10L,
                        email,
                        nickname,
                        "USER",
                        "ACTIVE"
                );

        when(authAccountRepository.existsByEmail(email))
                .thenReturn(false);

        when(passwordEncoder.encode(password))
                .thenReturn(encodedPassword);

        when(userServiceClient.createMember(
                any(CreateMemberInternalRequest.class)
        )).thenReturn(memberResponse);

        when(authAccountRepository.save(
                any(AuthAccount.class)
        )).thenThrow(
                new IllegalStateException(
                        "Auth 계정 저장 실패"
                )
        );

        // when & then
        assertThatThrownBy(
                () -> authCommandService.signUp(command)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Auth 계정 저장 실패");

        verify(userServiceClient)
                .rollbackSignUp(10L);
    }

    @Test
    @DisplayName("로그인 성공 시 닉네임이 포함된 Access Token과 Refresh Token을 발급한다")
    void loginSuccess() {
        // given
        String email = "test@example.com";
        String rawPassword = "Password123!";
        String encodedPassword = "encoded-password";
        String nickname = "테스터";

        AuthAccount authAccount =
                AuthAccount.create(
                        1L,
                        email,
                        encodedPassword
                );

        MemberAuthInfoResponse memberInfo =
                new MemberAuthInfoResponse(
                        1L,
                        email,
                        nickname,
                        "USER",
                        "ACTIVE",
                        true
                );

        when(authAccountRepository.findByEmail(email))
                .thenReturn(Optional.of(authAccount));

        when(passwordEncoder.matches(
                rawPassword,
                encodedPassword
        )).thenReturn(true);

        when(userServiceClient.getMemberAuthInfo(1L))
                .thenReturn(memberInfo);

        when(jwtProvider.createAccessToken(
                1L,
                "USER",
                nickname
        )).thenReturn("access-token");

        when(jwtProvider.createRefreshToken(
                1L,
                "USER"
        )).thenReturn("refresh-token");

        when(jwtProvider.getAccessTokenExpirationSeconds())
                .thenReturn(1800L);

        when(jwtProvider.getRefreshTokenExpirationSeconds())
                .thenReturn(1209600L);

        when(jwtProvider.getRefreshTokenExpiration())
                .thenReturn(Duration.ofDays(14));

        // when
        TokenResult result =
                authCommandService.login(
                        new LoginCommand(
                                email,
                                rawPassword
                        )
                );

        // then
        assertThat(result.accessToken())
                .isEqualTo("access-token");

        assertThat(result.refreshToken())
                .isEqualTo("refresh-token");

        assertThat(result.accessTokenExpiresIn())
                .isEqualTo(1800L);

        assertThat(result.refreshTokenExpiresIn())
                .isEqualTo(1209600L);

        verify(authAccountRepository)
                .findByEmail(email);

        verify(passwordEncoder)
                .matches(
                        rawPassword,
                        encodedPassword
                );

        verify(userServiceClient)
                .getMemberAuthInfo(1L);

        verify(jwtProvider)
                .createAccessToken(
                        1L,
                        "USER",
                        nickname
                );

        verify(jwtProvider)
                .createRefreshToken(
                        1L,
                        "USER"
                );

        verify(refreshTokenStore)
                .save(
                        1L,
                        "refresh-token",
                        Duration.ofDays(14)
                );
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 로그인에 실패한다")
    void loginFailsWhenPasswordDoesNotMatch() {
        // given
        String email = "test@example.com";
        String rawPassword = "WrongPassword!";
        String encodedPassword = "encoded-password";

        AuthAccount authAccount =
                AuthAccount.create(
                        1L,
                        email,
                        encodedPassword
                );

        when(authAccountRepository.findByEmail(email))
                .thenReturn(Optional.of(authAccount));

        when(passwordEncoder.matches(
                rawPassword,
                encodedPassword
        )).thenReturn(false);

        // when & then
        assertThatThrownBy(
                () -> authCommandService.login(
                        new LoginCommand(
                                email,
                                rawPassword
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                );

        verify(authAccountRepository)
                .findByEmail(email);

        verify(passwordEncoder)
                .matches(
                        rawPassword,
                        encodedPassword
                );

        verifyNoInteractions(
                userServiceClient,
                jwtProvider,
                refreshTokenStore
        );
    }

    @Test
    @DisplayName("로그인할 수 없는 회원 상태이면 로그인에 실패한다")
    void loginFailsWhenMemberIsNotAvailable() {
        // given
        String email = "suspended@example.com";
        String rawPassword = "Password123!";
        String encodedPassword = "encoded-password";

        AuthAccount authAccount =
                AuthAccount.create(
                        2L,
                        email,
                        encodedPassword
                );

        MemberAuthInfoResponse memberInfo =
                new MemberAuthInfoResponse(
                        2L,
                        email,
                        "정지회원",
                        "USER",
                        "SUSPENDED",
                        false
                );

        when(authAccountRepository.findByEmail(email))
                .thenReturn(Optional.of(authAccount));

        when(passwordEncoder.matches(
                rawPassword,
                encodedPassword
        )).thenReturn(true);

        when(userServiceClient.getMemberAuthInfo(2L))
                .thenReturn(memberInfo);

        // when & then
        assertThatThrownBy(
                () -> authCommandService.login(
                        new LoginCommand(
                                email,
                                rawPassword
                        )
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로그인할 수 없는 회원입니다.");

        verify(authAccountRepository)
                .findByEmail(email);

        verify(passwordEncoder)
                .matches(
                        rawPassword,
                        encodedPassword
                );

        verify(userServiceClient)
                .getMemberAuthInfo(2L);

        verifyNoInteractions(
                jwtProvider,
                refreshTokenStore
        );
    }

    @Test
    @DisplayName("유효한 Refresh Token이면 최신 닉네임으로 토큰을 재발급한다")
    void refreshSuccess() {
        // given
        String oldRefreshToken = "old-refresh-token";
        String nickname = "테스터";

        MemberAuthInfoResponse memberInfo =
                new MemberAuthInfoResponse(
                        1L,
                        "test@example.com",
                        nickname,
                        "USER",
                        "ACTIVE",
                        true
                );

        when(jwtProvider.getMemberId(oldRefreshToken))
                .thenReturn(1L);

        when(refreshTokenStore.findByMemberId(1L))
                .thenReturn(Optional.of(oldRefreshToken));

        when(userServiceClient.getMemberAuthInfo(1L))
                .thenReturn(memberInfo);

        when(jwtProvider.createAccessToken(
                1L,
                "USER",
                nickname
        )).thenReturn("new-access-token");

        when(jwtProvider.createRefreshToken(
                1L,
                "USER"
        )).thenReturn("new-refresh-token");

        when(jwtProvider.getAccessTokenExpirationSeconds())
                .thenReturn(1800L);

        when(jwtProvider.getRefreshTokenExpirationSeconds())
                .thenReturn(1209600L);

        when(jwtProvider.getRefreshTokenExpiration())
                .thenReturn(Duration.ofDays(14));

        // when
        TokenResult result =
                authCommandService.refresh(
                        new RefreshTokenCommand(
                                oldRefreshToken
                        )
                );

        // then
        assertThat(result.accessToken())
                .isEqualTo("new-access-token");

        assertThat(result.refreshToken())
                .isEqualTo("new-refresh-token");

        assertThat(result.accessTokenExpiresIn())
                .isEqualTo(1800L);

        assertThat(result.refreshTokenExpiresIn())
                .isEqualTo(1209600L);

        verify(jwtProvider)
                .validateRefreshToken(oldRefreshToken);

        verify(jwtProvider)
                .getMemberId(oldRefreshToken);

        verify(refreshTokenStore)
                .findByMemberId(1L);

        verify(userServiceClient)
                .getMemberAuthInfo(1L);

        verify(jwtProvider)
                .createAccessToken(
                        1L,
                        "USER",
                        nickname
                );

        verify(jwtProvider)
                .createRefreshToken(
                        1L,
                        "USER"
                );

        verify(refreshTokenStore)
                .save(
                        1L,
                        "new-refresh-token",
                        Duration.ofDays(14)
                );
    }

    @Test
    @DisplayName("Redis에 저장된 Refresh Token과 다르면 재발급에 실패한다")
    void refreshFailsWhenTokenDoesNotMatch() {
        // given
        String providedRefreshToken =
                "provided-refresh-token";

        String storedRefreshToken =
                "stored-refresh-token";

        when(jwtProvider.getMemberId(providedRefreshToken))
                .thenReturn(1L);

        when(refreshTokenStore.findByMemberId(1L))
                .thenReturn(Optional.of(storedRefreshToken));

        RefreshTokenCommand command =
                new RefreshTokenCommand(
                        providedRefreshToken
                );

        // when & then
        assertThatThrownBy(
                () -> authCommandService.refresh(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Refresh Token이 일치하지 않습니다."
                );

        verify(jwtProvider)
                .validateRefreshToken(
                        providedRefreshToken
                );

        verify(jwtProvider)
                .getMemberId(
                        providedRefreshToken
                );

        verify(refreshTokenStore)
                .findByMemberId(1L);

        verifyNoInteractions(userServiceClient);

        verify(refreshTokenStore, never())
                .save(
                        anyLong(),
                        any(String.class),
                        any(Duration.class)
                );
    }

    @Test
    @DisplayName("로그아웃 성공 시 Redis의 Refresh Token을 삭제한다")
    void logoutSuccess() {
        // given
        String refreshToken = "refresh-token";

        when(jwtProvider.getMemberId(refreshToken))
                .thenReturn(1L);

        when(refreshTokenStore.findByMemberId(1L))
                .thenReturn(Optional.of(refreshToken));

        // when
        authCommandService.logout(
                new LogoutCommand(refreshToken)
        );

        // then
        verify(jwtProvider)
                .validateRefreshToken(refreshToken);

        verify(jwtProvider)
                .getMemberId(refreshToken);

        verify(refreshTokenStore)
                .findByMemberId(1L);

        verify(refreshTokenStore)
                .deleteByMemberId(1L);
    }


    @Test
    @DisplayName("Redis에 저장된 Refresh Token이 없으면 재발급에 실패한다")
    void refreshFailsWhenStoredTokenDoesNotExist() {
        // given
        String refreshToken = "refresh-token";

        when(jwtProvider.getMemberId(refreshToken))
                .thenReturn(1L);

        when(refreshTokenStore.findByMemberId(1L))
                .thenReturn(Optional.empty());

        RefreshTokenCommand command =
                new RefreshTokenCommand(refreshToken);

        // when & then
        assertThatThrownBy(
                () -> authCommandService.refresh(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("저장된 Refresh Token이 없습니다.");

        verify(jwtProvider)
                .validateRefreshToken(refreshToken);

        verify(jwtProvider)
                .getMemberId(refreshToken);

        verify(refreshTokenStore)
                .findByMemberId(1L);

        verifyNoInteractions(userServiceClient);

        verify(jwtProvider, never())
                .createAccessToken(
                        anyLong(),
                        any(String.class),
                        any(String.class)
                );

        verify(jwtProvider, never())
                .createRefreshToken(
                        anyLong(),
                        any(String.class)
                );

        verify(refreshTokenStore, never())
                .save(
                        anyLong(),
                        any(String.class),
                        any(Duration.class)
                );
    }

    @Test
    @DisplayName("Redis에 저장된 Refresh Token이 없으면 로그아웃에 실패한다")
    void logoutFailsWhenStoredTokenDoesNotExist() {
        // given
        String refreshToken = "refresh-token";

        when(jwtProvider.getMemberId(refreshToken))
                .thenReturn(1L);

        when(refreshTokenStore.findByMemberId(1L))
                .thenReturn(Optional.empty());

        LogoutCommand command =
                new LogoutCommand(refreshToken);

        // when & then
        assertThatThrownBy(
                () -> authCommandService.logout(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "이미 로그아웃되었거나 저장된 토큰이 없습니다."
                );

        verify(jwtProvider)
                .validateRefreshToken(refreshToken);

        verify(jwtProvider)
                .getMemberId(refreshToken);

        verify(refreshTokenStore)
                .findByMemberId(1L);

        verify(refreshTokenStore, never())
                .deleteByMemberId(anyLong());

        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("요청 Refresh Token과 Redis 토큰이 다르면 로그아웃에 실패한다")
    void logoutFailsWhenTokenDoesNotMatch() {
        // given
        String providedRefreshToken =
                "provided-refresh-token";

        String storedRefreshToken =
                "stored-refresh-token";

        when(jwtProvider.getMemberId(providedRefreshToken))
                .thenReturn(1L);

        when(refreshTokenStore.findByMemberId(1L))
                .thenReturn(Optional.of(storedRefreshToken));

        LogoutCommand command =
                new LogoutCommand(providedRefreshToken);

        // when & then
        assertThatThrownBy(
                () -> authCommandService.logout(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token이 일치하지 않습니다.");

        verify(jwtProvider)
                .validateRefreshToken(providedRefreshToken);

        verify(jwtProvider)
                .getMemberId(providedRefreshToken);

        verify(refreshTokenStore)
                .findByMemberId(1L);

        verify(refreshTokenStore, never())
                .deleteByMemberId(anyLong());

        verifyNoInteractions(userServiceClient);
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
    void loginFailsWhenEmailDoesNotExist() {
        // given
        String email = "not-found@example.com";
        String password = "Password123!";

        when(authAccountRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        LoginCommand command =
                new LoginCommand(
                        email,
                        password
                );

        // when & then
        assertThatThrownBy(
                () -> authCommandService.login(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                );

        verify(authAccountRepository)
                .findByEmail(email);

        verifyNoInteractions(
                passwordEncoder,
                userServiceClient,
                jwtProvider,
                refreshTokenStore
        );
    }

    @Test
    @DisplayName("재발급 시 로그인할 수 없는 회원이면 토큰 발급에 실패한다")
    void refreshFailsWhenMemberIsNotAvailable() {
        // given
        String refreshToken = "refresh-token";

        MemberAuthInfoResponse memberInfo =
                new MemberAuthInfoResponse(
                        2L,
                        "suspended@example.com",
                        "정지회원",
                        "USER",
                        "SUSPENDED",
                        false
                );

        when(jwtProvider.getMemberId(refreshToken))
                .thenReturn(2L);

        when(refreshTokenStore.findByMemberId(2L))
                .thenReturn(Optional.of(refreshToken));

        when(userServiceClient.getMemberAuthInfo(2L))
                .thenReturn(memberInfo);

        RefreshTokenCommand command =
                new RefreshTokenCommand(refreshToken);

        // when & then
        assertThatThrownBy(
                () -> authCommandService.refresh(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로그인할 수 없는 회원입니다.");

        verify(jwtProvider)
                .validateRefreshToken(refreshToken);

        verify(jwtProvider)
                .getMemberId(refreshToken);

        verify(refreshTokenStore)
                .findByMemberId(2L);

        verify(userServiceClient)
                .getMemberAuthInfo(2L);

        verify(jwtProvider, never())
                .createAccessToken(
                        anyLong(),
                        any(String.class),
                        any(String.class)
                );

        verify(jwtProvider, never())
                .createRefreshToken(
                        anyLong(),
                        any(String.class)
                );

        verify(refreshTokenStore, never())
                .save(
                        anyLong(),
                        any(String.class),
                        any(Duration.class)
                );
    }
}