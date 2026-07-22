package com.bubbletea.gateway.authentication.jwt;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayJwtProviderTest {

    private static final String SECRET =
            "bubbletea-gateway-jwt-provider-test-secret-key-must-be-longer-than-32-bytes";

    private static final String OTHER_SECRET =
            "bubbletea-gateway-other-test-secret-key-must-be-longer-than-32-bytes";

    private static final String ROLE_CLAIM =
            "role";

    private static final String NICKNAME_CLAIM =
            "nickname";

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
    @DisplayName("정상 Access Token을 검증하고 회원 정보를 반환한다")
    void validateAccessTokenSuccess() {
        // given
        Long memberId = 1L;
        String role = "USER";
        String nickname = "테스터";

        String accessToken =
                createToken(
                        String.valueOf(memberId),
                        role,
                        nickname,
                        "ACCESS",
                        secretKey,
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        // when
        JwtClaims jwtClaims =
                gatewayJwtProvider.validateAccessToken(
                        accessToken
                );

        // then
        assertThat(jwtClaims.userId())
                .isEqualTo(memberId);

        assertThat(jwtClaims.role())
                .isEqualTo(role);

        assertThat(jwtClaims.nickname())
                .isEqualTo(nickname);
    }

    @Test
    @DisplayName("Refresh Token을 Access Token으로 검증하면 실패한다")
    void refreshTokenFailsAccessTokenValidation() {
        // given
        String refreshToken =
                createToken(
                        "1",
                        "USER",
                        null,
                        "REFRESH",
                        secretKey,
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        // when & then
        assertThatThrownBy(
                () -> gatewayJwtProvider.validateAccessToken(
                        refreshToken
                )
        )
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("Access Token이 아닙니다.");
    }

    @Test
    @DisplayName("nickname 클레임이 없으면 Access Token 검증에 실패한다")
    void missingNicknameFailsValidation() {
        // given
        String accessToken =
                createToken(
                        "1",
                        "USER",
                        null,
                        "ACCESS",
                        secretKey,
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        // when & then
        assertThatThrownBy(
                () -> gatewayJwtProvider.validateAccessToken(
                        accessToken
                )
        )
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("토큰에 회원 닉네임이 없습니다.");
    }

    @Test
    @DisplayName("빈 nickname 클레임이면 Access Token 검증에 실패한다")
    void blankNicknameFailsValidation() {
        // given
        String accessToken =
                createToken(
                        "1",
                        "USER",
                        " ",
                        "ACCESS",
                        secretKey,
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        // when & then
        assertThatThrownBy(
                () -> gatewayJwtProvider.validateAccessToken(
                        accessToken
                )
        )
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("토큰에 회원 닉네임이 없습니다.");
    }

    @Test
    @DisplayName("role 클레임이 없으면 Access Token 검증에 실패한다")
    void missingRoleFailsValidation() {
        // given
        String accessToken =
                createToken(
                        "1",
                        null,
                        "테스터",
                        "ACCESS",
                        secretKey,
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        // when & then
        assertThatThrownBy(
                () -> gatewayJwtProvider.validateAccessToken(
                        accessToken
                )
        )
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("토큰에 회원 역할이 없습니다.");
    }

    @Test
    @DisplayName("subject가 없으면 Access Token 검증에 실패한다")
    void missingSubjectFailsValidation() {
        // given
        String accessToken =
                createToken(
                        null,
                        "USER",
                        "테스터",
                        "ACCESS",
                        secretKey,
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        // when & then
        assertThatThrownBy(
                () -> gatewayJwtProvider.validateAccessToken(
                        accessToken
                )
        )
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("토큰에 회원 ID가 없습니다.");
    }

    @Test
    @DisplayName("회원 ID가 숫자가 아니면 Access Token 검증에 실패한다")
    void invalidSubjectFailsValidation() {
        // given
        String accessToken =
                createToken(
                        "invalid-member-id",
                        "USER",
                        "테스터",
                        "ACCESS",
                        secretKey,
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        // when & then
        assertThatThrownBy(
                () -> gatewayJwtProvider.validateAccessToken(
                        accessToken
                )
        )
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage(
                        "토큰의 회원 ID 형식이 올바르지 않습니다."
                );
    }

    @Test
    @DisplayName("만료된 Access Token은 검증에 실패한다")
    void expiredAccessTokenFailsValidation() {
        // given
        Instant issuedAt =
                Instant.now().minusSeconds(7200);

        Instant expiration =
                Instant.now().minusSeconds(3600);

        String accessToken =
                createToken(
                        "1",
                        "USER",
                        "테스터",
                        "ACCESS",
                        secretKey,
                        issuedAt,
                        expiration
                );

        // when & then
        assertThatThrownBy(
                () -> gatewayJwtProvider.validateAccessToken(
                        accessToken
                )
        )
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("만료된 Access Token입니다.");
    }

    @Test
    @DisplayName("다른 Secret으로 서명한 Access Token은 검증에 실패한다")
    void invalidSignatureFailsValidation() {
        // given
        SecretKey otherSecretKey =
                Keys.hmacShaKeyFor(
                        OTHER_SECRET.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        String accessToken =
                createToken(
                        "1",
                        "USER",
                        "테스터",
                        "ACCESS",
                        otherSecretKey,
                        Instant.now(),
                        Instant.now().plusSeconds(3600)
                );

        // when & then
        assertThatThrownBy(
                () -> gatewayJwtProvider.validateAccessToken(
                        accessToken
                )
        )
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("유효하지 않은 Access Token입니다.");
    }

    @Test
    @DisplayName("빈 Access Token은 검증에 실패한다")
    void blankAccessTokenFailsValidation() {
        // when & then
        assertThatThrownBy(
                () -> gatewayJwtProvider.validateAccessToken(
                        " "
                )
        )
                .isInstanceOf(InvalidJwtException.class)
                .hasMessage("Access Token이 비어 있습니다.");
    }

    @Test
    @DisplayName("JWT Secret이 비어 있으면 생성에 실패한다")
    void blankSecretFailsProviderCreation() {
        assertThatThrownBy(
                () -> new GatewayJwtProvider(" ")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT Secret 설정이 필요합니다.");
    }

    @Test
    @DisplayName("JWT Secret이 32바이트 미만이면 생성에 실패한다")
    void shortSecretFailsProviderCreation() {
        assertThatThrownBy(
                () -> new GatewayJwtProvider(
                        "short-secret"
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "JWT Secret은 32바이트 이상이어야 합니다."
                );
    }

    private String createToken(
            String subject,
            String role,
            String nickname,
            String tokenType,
            SecretKey signingKey,
            Instant issuedAt,
            Instant expiration
    ) {
        JwtBuilder builder =
                Jwts.builder()
                        .claim(
                                TOKEN_TYPE_CLAIM,
                                tokenType
                        )
                        .issuedAt(
                                Date.from(issuedAt)
                        )
                        .expiration(
                                Date.from(expiration)
                        );

        if (subject != null) {
            builder.subject(subject);
        }

        if (role != null) {
            builder.claim(
                    ROLE_CLAIM,
                    role
            );
        }

        if (nickname != null) {
            builder.claim(
                    NICKNAME_CLAIM,
                    nickname
            );
        }

        return builder
                .signWith(signingKey)
                .compact();
    }
}