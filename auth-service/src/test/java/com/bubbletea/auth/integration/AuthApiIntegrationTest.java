package com.bubbletea.auth.integration;

import com.bubbletea.auth.application.auth.AuthCommandService;
import com.bubbletea.auth.application.auth.command.LoginCommand;
import com.bubbletea.auth.domain.auth.AuthAccount;
import com.bubbletea.auth.domain.auth.AuthAccountRepository;
import com.bubbletea.auth.domain.auth.RefreshTokenStore;
import com.bubbletea.auth.infrastructure.client.UserServiceClient;
import com.bubbletea.auth.infrastructure.client.dto.CreateMemberInternalRequest;
import com.bubbletea.auth.infrastructure.client.dto.CreateMemberInternalResponse;
import com.bubbletea.auth.infrastructure.client.dto.MemberAuthInfoResponse;
import com.bubbletea.auth.infrastructure.jwt.JwtProvider;
import com.bubbletea.auth.presentation.auth.RefreshTokenCookieManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthApiIntegrationTest {

    private static final String EMAIL = "integration@example.com";
    private static final String RAW_PASSWORD = "Password123!";
    private static final Long MEMBER_ID = 1L;
    private static final String ROLE = "USER";
    private static final String NICKNAME = "통합테스터";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthCommandService authCommandService;

    @Autowired
    private AuthAccountRepository authAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM auth_accounts");
    }


    @Test
    @DisplayName("회원가입 성공 시 User Service에 회원을 생성하고 Auth DB에 인증 계정을 저장한다")
    void signUpSuccess() throws Exception {
        // given
        CreateMemberInternalResponse memberResponse =
                new CreateMemberInternalResponse(
                        MEMBER_ID,
                        EMAIL,
                        NICKNAME,
                        ROLE,
                        "ACTIVE"
                );

        when(userServiceClient.createMember(
                any(CreateMemberInternalRequest.class)
        )).thenReturn(memberResponse);

        // when & then
        mockMvc.perform(
                        post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "integration@example.com",
                                          "password": "Password123!",
                                          "nickname": "통합테스터"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(MEMBER_ID))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.nickname").value(NICKNAME));

        ArgumentCaptor<CreateMemberInternalRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        CreateMemberInternalRequest.class
                );

        verify(userServiceClient)
                .createMember(requestCaptor.capture());

        CreateMemberInternalRequest capturedRequest =
                requestCaptor.getValue();

        assertThat(capturedRequest.email())
                .isEqualTo(EMAIL);

        assertThat(capturedRequest.nickname())
                .isEqualTo(NICKNAME);

        AuthAccount savedAccount =
                authAccountRepository.findByEmail(EMAIL)
                        .orElseThrow();

        assertThat(savedAccount.getMemberId())
                .isEqualTo(MEMBER_ID);

        assertThat(savedAccount.getPasswordHash())
                .isNotEqualTo(RAW_PASSWORD);

        assertThat(
                passwordEncoder.matches(
                        RAW_PASSWORD,
                        savedAccount.getPasswordHash()
                )
        ).isTrue();

        verify(userServiceClient, never())
                .rollbackSignUp(anyLong());
    }

    @Test
    @DisplayName("로그인 성공 시 Access Token 응답과 Refresh Token 쿠키를 발급한다")
    void loginSuccess() throws Exception {
        AuthAccount authAccount = AuthAccount.create(
                MEMBER_ID,
                EMAIL,
                passwordEncoder.encode(RAW_PASSWORD)
        );

        authAccountRepository.save(authAccount);

        MemberAuthInfoResponse memberInfo =
                new MemberAuthInfoResponse(
                        MEMBER_ID,
                        EMAIL,
                        NICKNAME,
                        ROLE,
                        "ACTIVE",
                        true
                );

        when(userServiceClient.getMemberAuthInfo(MEMBER_ID))
                .thenReturn(memberInfo);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "integration@example.com",
                                          "password": "Password123!"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.accessToken").value(not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(cookie().exists(
                        RefreshTokenCookieManager.COOKIE_NAME
                ))
                .andExpect(cookie().httpOnly(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        true
                ))
                .andExpect(cookie().path(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        "/api/auth"
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("refreshToken=")
                ));

        verify(userServiceClient)
                .getMemberAuthInfo(MEMBER_ID);

        verify(refreshTokenStore)
                .save(
                        eq(MEMBER_ID),
                        anyString(),
                        any(Duration.class)
                );
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 로그인에 실패한다")
    void loginFailsWhenEmailDoesNotExist() {
        LoginCommand command =
                new LoginCommand(
                        "not-found@example.com",
                        RAW_PASSWORD
                );

        assertThatThrownBy(
                () -> authCommandService.login(command)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                );

        verify(userServiceClient, never())
                .getMemberAuthInfo(anyLong());

        verify(refreshTokenStore, never())
                .save(
                        anyLong(),
                        anyString(),
                        any(Duration.class)
                );
    }

    @Test
    @DisplayName("유효한 Refresh Token이면 토큰을 재발급하고 쿠키를 교체한다")
    void refreshSuccess() throws Exception {
        String oldRefreshToken =
                jwtProvider.createRefreshToken(
                        MEMBER_ID,
                        ROLE
                );

        MemberAuthInfoResponse memberInfo =
                new MemberAuthInfoResponse(
                        MEMBER_ID,
                        EMAIL,
                        NICKNAME,
                        ROLE,
                        "ACTIVE",
                        true
                );

        when(refreshTokenStore.findByMemberId(MEMBER_ID))
                .thenReturn(Optional.of(oldRefreshToken));

        when(userServiceClient.getMemberAuthInfo(MEMBER_ID))
                .thenReturn(memberInfo);

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(
                                        new Cookie(
                                                RefreshTokenCookieManager.COOKIE_NAME,
                                                oldRefreshToken
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.accessToken").value(not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(cookie().exists(
                        RefreshTokenCookieManager.COOKIE_NAME
                ))
                .andExpect(cookie().httpOnly(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        true
                ))
                .andExpect(cookie().path(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        "/api/auth"
                ));

        verify(refreshTokenStore)
                .findByMemberId(MEMBER_ID);

        verify(userServiceClient)
                .getMemberAuthInfo(MEMBER_ID);

        verify(refreshTokenStore)
                .save(
                        eq(MEMBER_ID),
                        anyString(),
                        any(Duration.class)
                );
    }

    @Test
    @DisplayName("로그아웃 성공 시 Refresh Token을 삭제하고 쿠키를 만료한다")
    void logoutSuccess() throws Exception {
        String refreshToken =
                jwtProvider.createRefreshToken(
                        MEMBER_ID,
                        ROLE
                );

        when(refreshTokenStore.findByMemberId(MEMBER_ID))
                .thenReturn(Optional.of(refreshToken));

        mockMvc.perform(
                        post("/api/auth/logout")
                                .cookie(
                                        new Cookie(
                                                RefreshTokenCookieManager.COOKIE_NAME,
                                                refreshToken
                                        )
                                )
                )
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        0
                ))
                .andExpect(cookie().path(
                        RefreshTokenCookieManager.COOKIE_NAME,
                        "/api/auth"
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=0")
                ));

        verify(refreshTokenStore)
                .findByMemberId(MEMBER_ID);

        verify(refreshTokenStore)
                .deleteByMemberId(MEMBER_ID);

        verify(refreshTokenStore, never())
                .save(
                        anyLong(),
                        anyString(),
                        any(Duration.class)
                );
    }
}