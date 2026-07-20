package com.bubbletea.gateway.authentication.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayJwtProviderTest {

    private static final String SECRET =
            "bubbletea-gateway-test-secret-key-must-be-longer-than-32-bytes";

    private static final String DIFFERENT_SECRET =
            "bubbletea-different-test-secret-key-must-be-longer-than-32-bytes";

    private static final String ROLE_CLAIM =
            "role";

    private static final String TOKEN_TYPE_CLAIM =
            "tokenType";

    private GatewayJwtProvider gatewayJwtProvider;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        gatewayJwtProvider =
                new GatewayJwtProvider(SECRET);

        secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("정상 Access Token을 검증하면 회원 ID와 역할을 반환한다")
    void validateAccessTokenSuccess() {
        // given
        String accessToken = createToken(
                1L,
                "USER",
                "ACCESS",
                secretKey,
                Instant.now().plusSeconds(3600)
        );

        // when
        JwtClaims result =
                gatewayJwtProvider.validateAccessToken(
                        accessToken
                );

        // then
        assertEquals(1L, result.userId());
        assertEquals("USER", result.role());
    }

    @Test
    @DisplayName("Refresh Token을 Access Token으로 사용하면 예외가 발생한다")
    void rejectRefreshToken() {
        // given
        String refreshToken = createToken(
                1L,
                "USER",
                "REFRESH",
                secretKey,
                Instant.now().plusSeconds(3600)
        );

        // when
        InvalidJwtException exception =
                assertThrows(
                        InvalidJwtException.class,
                        () -> gatewayJwtProvider
                                .validateAccessToken(
                                        refreshToken
                                )
                );

        // then
        assertEquals(
                "Access Token이 아닙니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("만료된 Access Token은 거부한다")
    void rejectExpiredAccessToken() {
        // given
        String expiredAccessToken = createToken(
                1L,
                "USER",
                "ACCESS",
                secretKey,
                Instant.now().minusSeconds(60)
        );

        // when
        InvalidJwtException exception =
                assertThrows(
                        InvalidJwtException.class,
                        () -> gatewayJwtProvider
                                .validateAccessToken(
                                        expiredAccessToken
                                )
                );

        // then
        assertEquals(
                "만료된 Access Token입니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("다른 Secret으로 서명한 Access Token은 거부한다")
    void rejectInvalidSignatureToken() {
        // given
        SecretKey differentSecretKey =
                Keys.hmacShaKeyFor(
                        DIFFERENT_SECRET.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        String invalidSignatureToken = createToken(
                1L,
                "USER",
                "ACCESS",
                differentSecretKey,
                Instant.now().plusSeconds(3600)
        );

        // when
        InvalidJwtException exception =
                assertThrows(
                        InvalidJwtException.class,
                        () -> gatewayJwtProvider
                                .validateAccessToken(
                                        invalidSignatureToken
                                )
                );

        // then
        assertEquals(
                "유효하지 않은 Access Token입니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("role Claim이 없는 Access Token은 거부한다")
    void rejectTokenWithoutRole() {
        // given
        Instant now = Instant.now();

        String tokenWithoutRole =
                Jwts.builder()
                        .subject("1")
                        .claim(
                                TOKEN_TYPE_CLAIM,
                                "ACCESS"
                        )
                        .issuedAt(Date.from(now))
                        .expiration(
                                Date.from(
                                        now.plusSeconds(3600)
                                )
                        )
                        .signWith(secretKey)
                        .compact();

        // when
        InvalidJwtException exception =
                assertThrows(
                        InvalidJwtException.class,
                        () -> gatewayJwtProvider
                                .validateAccessToken(
                                        tokenWithoutRole
                                )
                );

        // then
        assertEquals(
                "토큰에 회원 역할이 없습니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("회원 ID가 숫자가 아닌 Access Token은 거부한다")
    void rejectTokenWithInvalidMemberId() {
        // given
        Instant now = Instant.now();

        String invalidMemberIdToken =
                Jwts.builder()
                        .subject("invalid-member-id")
                        .claim(
                                ROLE_CLAIM,
                                "USER"
                        )
                        .claim(
                                TOKEN_TYPE_CLAIM,
                                "ACCESS"
                        )
                        .issuedAt(Date.from(now))
                        .expiration(
                                Date.from(
                                        now.plusSeconds(3600)
                                )
                        )
                        .signWith(secretKey)
                        .compact();

        // when
        InvalidJwtException exception =
                assertThrows(
                        InvalidJwtException.class,
                        () -> gatewayJwtProvider
                                .validateAccessToken(
                                        invalidMemberIdToken
                                )
                );

        // then
        assertEquals(
                "토큰의 회원 ID 형식이 올바르지 않습니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("빈 Access Token은 거부한다")
    void rejectBlankToken() {
        // when
        InvalidJwtException exception =
                assertThrows(
                        InvalidJwtException.class,
                        () -> gatewayJwtProvider
                                .validateAccessToken(" ")
                );

        // then
        assertTrue(
                exception.getMessage()
                        .contains("비어 있습니다")
        );
    }

    private String createToken(
            Long memberId,
            String role,
            String tokenType,
            SecretKey signingKey,
            Instant expiration
    ) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(
                        ROLE_CLAIM,
                        role
                )
                .claim(
                        TOKEN_TYPE_CLAIM,
                        tokenType
                )
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }
}