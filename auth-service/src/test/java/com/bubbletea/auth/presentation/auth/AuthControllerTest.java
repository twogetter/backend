package com.bubbletea.auth.presentation.auth;

import com.bubbletea.auth.application.auth.AuthCommandService;
import com.bubbletea.auth.application.auth.command.LoginCommand;
import com.bubbletea.auth.application.auth.command.LogoutCommand;
import com.bubbletea.auth.application.auth.command.RefreshTokenCommand;
import com.bubbletea.auth.application.auth.command.SignUpCommand;
import com.bubbletea.auth.application.auth.result.SignUpResult;
import com.bubbletea.auth.application.auth.result.TokenResult;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthCommandService authCommandService;

    @Mock
    private RefreshTokenCookieManager cookieManager;

    @BeforeEach
    void setUp() {
        AuthController authController =
                new AuthController(
                        authCommandService,
                        cookieManager
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공 시 201 상태와 회원 정보를 반환한다")
    void signUpSuccess() throws Exception {
        // given
        SignUpResult result = new SignUpResult(
                1L,
                "test@example.com",
                "테스터"
        );

        when(authCommandService.signUp(
                any(SignUpCommand.class)
        )).thenReturn(result);

        String requestBody = """
                {
                  "email": "test@example.com",
                  "password": "Password123!",
                  "nickname": "테스터"
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.memberId").value(1L)
                )
                .andExpect(
                        jsonPath("$.email").value(
                                "test@example.com"
                        )
                )
                .andExpect(
                        jsonPath("$.nickname").value(
                                "테스터"
                        )
                );

        ArgumentCaptor<SignUpCommand> commandCaptor =
                ArgumentCaptor.forClass(
                        SignUpCommand.class
                );

        verify(authCommandService)
                .signUp(commandCaptor.capture());

        SignUpCommand command =
                commandCaptor.getValue();

        assertThat(command.email())
                .isEqualTo("test@example.com");

        assertThat(command.password())
                .isEqualTo("Password123!");

        assertThat(command.nickname())
                .isEqualTo("테스터");
    }

    @Test
    @DisplayName("회원가입 이메일 형식이 잘못되면 400 상태를 반환한다")
    void signUpFailsWhenEmailIsInvalid() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "invalid-email",
                  "password": "Password123!",
                  "nickname": "테스터"
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        verify(authCommandService, never())
                .signUp(any(SignUpCommand.class));
    }

    @Test
    @DisplayName("로그인 성공 시 Access Token과 Refresh Token 쿠키를 반환한다")
    void loginSuccess() throws Exception {
        // given
        TokenResult result = new TokenResult(
                "access-token",
                "refresh-token",
                1800L,
                1209600L
        );

        ResponseCookie responseCookie =
                ResponseCookie.from(
                                RefreshTokenCookieManager.COOKIE_NAME,
                                "refresh-token"
                        )
                        .httpOnly(true)
                        .path("/api/auth")
                        .maxAge(Duration.ofDays(14))
                        .sameSite("Lax")
                        .build();

        when(authCommandService.login(
                any(LoginCommand.class)
        )).thenReturn(result);

        when(cookieManager.create(
                "refresh-token",
                1209600L
        )).thenReturn(responseCookie);

        String requestBody = """
                {
                  "email": "test@example.com",
                  "password": "Password123!"
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.accessToken").value(
                                "access-token"
                        )
                )
                .andExpect(
                        jsonPath("$.tokenType").value(
                                "Bearer"
                        )
                )
                .andExpect(
                        jsonPath("$.expiresIn").value(
                                1800L
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString(
                                        "refreshToken=refresh-token"
                                )
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString("HttpOnly")
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString(
                                        "Path=/api/auth"
                                )
                        )
                );

        ArgumentCaptor<LoginCommand> commandCaptor =
                ArgumentCaptor.forClass(
                        LoginCommand.class
                );

        verify(authCommandService)
                .login(commandCaptor.capture());

        LoginCommand command =
                commandCaptor.getValue();

        assertThat(command.email())
                .isEqualTo("test@example.com");

        assertThat(command.password())
                .isEqualTo("Password123!");

        verify(cookieManager)
                .create(
                        "refresh-token",
                        1209600L
                );
    }

    @Test
    @DisplayName("로그인 비밀번호가 비어 있으면 400 상태를 반환한다")
    void loginFailsWhenPasswordIsBlank() throws Exception {
        // given
        String requestBody = """
                {
                  "email": "test@example.com",
                  "password": ""
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        verify(authCommandService, never())
                .login(any(LoginCommand.class));
    }

    @Test
    @DisplayName("토큰 재발급 성공 시 새로운 토큰과 쿠키를 반환한다")
    void refreshSuccess() throws Exception {
        // given
        TokenResult result = new TokenResult(
                "new-access-token",
                "new-refresh-token",
                1800L,
                1209600L
        );

        ResponseCookie responseCookie =
                ResponseCookie.from(
                                RefreshTokenCookieManager.COOKIE_NAME,
                                "new-refresh-token"
                        )
                        .httpOnly(true)
                        .path("/api/auth")
                        .maxAge(Duration.ofDays(14))
                        .sameSite("Lax")
                        .build();

        when(authCommandService.refresh(
                any(RefreshTokenCommand.class)
        )).thenReturn(result);

        when(cookieManager.create(
                "new-refresh-token",
                1209600L
        )).thenReturn(responseCookie);

        Cookie requestCookie =
                new Cookie(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        "old-refresh-token"
                );

        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(requestCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken").value(
                                "new-access-token"
                        )
                )
                .andExpect(
                        jsonPath("$.tokenType").value(
                                "Bearer"
                        )
                )
                .andExpect(
                        jsonPath("$.expiresIn").value(
                                1800L
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString(
                                        "refreshToken=new-refresh-token"
                                )
                        )
                );

        ArgumentCaptor<RefreshTokenCommand> commandCaptor =
                ArgumentCaptor.forClass(
                        RefreshTokenCommand.class
                );

        verify(authCommandService)
                .refresh(commandCaptor.capture());

        RefreshTokenCommand command =
                commandCaptor.getValue();

        assertThat(command.refreshToken())
                .isEqualTo("old-refresh-token");

        verify(cookieManager)
                .create(
                        "new-refresh-token",
                        1209600L
                );
    }

    @Test
    @DisplayName("Refresh Token 쿠키가 없으면 재발급 요청은 400 상태를 반환한다")
    void refreshFailsWhenCookieIsMissing() throws Exception {
        // when & then
        mockMvc.perform(
                        post("/api/auth/refresh")
                )
                .andExpect(status().isBadRequest());

        verify(authCommandService, never())
                .refresh(
                        any(RefreshTokenCommand.class)
                );
    }

    @Test
    @DisplayName("로그아웃 성공 시 Refresh Token 쿠키를 삭제하고 204 상태를 반환한다")
    void logoutSuccess() throws Exception {
        // given
        ResponseCookie deletedCookie =
                ResponseCookie.from(
                                RefreshTokenCookieManager.COOKIE_NAME,
                                ""
                        )
                        .httpOnly(true)
                        .path("/api/auth")
                        .maxAge(Duration.ZERO)
                        .sameSite("Lax")
                        .build();

        when(cookieManager.delete())
                .thenReturn(deletedCookie);

        Cookie requestCookie =
                new Cookie(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        "refresh-token"
                );

        // when & then
        mockMvc.perform(
                        post("/api/auth/logout")
                                .cookie(requestCookie)
                )
                .andExpect(status().isNoContent())
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString(
                                        "refreshToken="
                                )
                        )
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString(
                                        "Max-Age=0"
                                )
                        )
                );

        ArgumentCaptor<LogoutCommand> commandCaptor =
                ArgumentCaptor.forClass(
                        LogoutCommand.class
                );

        verify(authCommandService)
                .logout(commandCaptor.capture());

        LogoutCommand command =
                commandCaptor.getValue();

        assertThat(command.refreshToken())
                .isEqualTo("refresh-token");

        verify(cookieManager)
                .delete();
    }
}