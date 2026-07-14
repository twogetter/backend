package com.bubbletea.auth.infrastructure.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String SECRET =
            "bubbletea-auth-jwt-test-secret-key-must-be-long-enough-2026";

    private static final long ACCESS_TOKEN_EXPIRATION =
            30 * 60 * 1000L;

    private static final long REFRESH_TOKEN_EXPIRATION =
            14 * 24 * 60 * 60 * 1000L;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties =
                new JwtProperties(
                        SECRET,
                        ACCESS_TOKEN_EXPIRATION,
                        REFRESH_TOKEN_EXPIRATION
                );

        jwtProvider = new JwtProvider(jwtProperties);
    }

    @Test
    @DisplayName("Access Token을 생성하고 회원 ID와 권한을 조회한다")
    void createAccessTokenSuccess() {
        // given
        Long memberId = 1L;
        String role = "USER";

        // when
        String accessToken =
                jwtProvider.createAccessToken(
                        memberId,
                        role
                );

        // then
        assertThat(accessToken)
                .isNotBlank();

        assertThat(jwtProvider.isValid(accessToken))
                .isTrue();

        assertThat(jwtProvider.getMemberId(accessToken))
                .isEqualTo(memberId);

        assertThat(jwtProvider.getRole(accessToken))
                .isEqualTo(role);

        assertThat(jwtProvider.getTokenType(accessToken))
                .isEqualTo(TokenType.ACCESS);
    }

    @Test
    @DisplayName("Refresh Token을 생성하고 토큰 타입을 조회한다")
    void createRefreshTokenSuccess() {
        // given
        Long memberId = 2L;
        String role = "ARTIST";

        // when
        String refreshToken =
                jwtProvider.createRefreshToken(
                        memberId,
                        role
                );

        // then
        assertThat(refreshToken)
                .isNotBlank();

        assertThat(jwtProvider.isValid(refreshToken))
                .isTrue();

        assertThat(jwtProvider.getMemberId(refreshToken))
                .isEqualTo(memberId);

        assertThat(jwtProvider.getRole(refreshToken))
                .isEqualTo(role);

        assertThat(jwtProvider.getTokenType(refreshToken))
                .isEqualTo(TokenType.REFRESH);
    }

    @Test
    @DisplayName("Refresh Token 검증에 성공한다")
    void validateRefreshTokenSuccess() {
        // given
        String refreshToken =
                jwtProvider.createRefreshToken(
                        1L,
                        "USER"
                );

        // when & then
        jwtProvider.validateRefreshToken(refreshToken);

        assertThat(jwtProvider.getTokenType(refreshToken))
                .isEqualTo(TokenType.REFRESH);
    }

    @Test
    @DisplayName("Access Token을 Refresh Token으로 검증하면 실패한다")
    void validateRefreshTokenFailsWhenAccessTokenIsProvided() {
        // given
        String accessToken =
                jwtProvider.createAccessToken(
                        1L,
                        "USER"
                );

        // when & then
        assertThatThrownBy(
                () -> jwtProvider.validateRefreshToken(
                        accessToken
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh Token이 아닙니다.");
    }

    @Test
    @DisplayName("변조된 JWT는 유효하지 않다")
    void tamperedTokenIsInvalid() {
        // given
        String accessToken =
                jwtProvider.createAccessToken(
                        1L,
                        "USER"
                );

        String tamperedToken =
                accessToken.substring(
                        0,
                        accessToken.length() - 2
                ) + "xx";

        // when
        boolean valid =
                jwtProvider.isValid(tamperedToken);

        // then
        assertThat(valid)
                .isFalse();
    }

    @Test
    @DisplayName("Access Token 만료 시간을 초 단위로 반환한다")
    void getAccessTokenExpirationSeconds() {
        // when
        long expirationSeconds =
                jwtProvider.getAccessTokenExpirationSeconds();

        // then
        assertThat(expirationSeconds)
                .isEqualTo(1800L);
    }

    @Test
    @DisplayName("Refresh Token 만료 시간을 초와 Duration으로 반환한다")
    void getRefreshTokenExpiration() {
        // when
        long expirationSeconds =
                jwtProvider.getRefreshTokenExpirationSeconds();

        Duration expirationDuration =
                jwtProvider.getRefreshTokenExpiration();

        // then
        assertThat(expirationSeconds)
                .isEqualTo(1209600L);

        assertThat(expirationDuration)
                .isEqualTo(Duration.ofDays(14));
    }

    @Test
    @DisplayName("발급된 토큰의 남은 만료 시간은 0보다 크다")
    void getRemainingExpirationSuccess() {
        // given
        String refreshToken =
                jwtProvider.createRefreshToken(
                        1L,
                        "USER"
                );

        // when
        Duration remainingExpiration =
                jwtProvider.getRemainingExpiration(
                        refreshToken
                );

        // then
        assertThat(remainingExpiration)
                .isPositive();

        assertThat(remainingExpiration)
                .isLessThanOrEqualTo(
                        Duration.ofDays(14)
                );
    }
}