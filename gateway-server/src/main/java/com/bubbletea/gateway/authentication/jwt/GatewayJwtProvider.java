package com.bubbletea.gateway.authentication.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class GatewayJwtProvider {

    private static final String ROLE_CLAIM =
            "role";

    private static final String NICKNAME_CLAIM =
            "nickname";

    private static final String TOKEN_TYPE_CLAIM =
            "tokenType";

    private static final String ACCESS_TOKEN_TYPE =
            "ACCESS";

    private final SecretKey secretKey;

    public GatewayJwtProvider(
            @Value("${jwt.secret}") String secret
    ) {
        validateSecret(secret);

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Access Token을 검증하고
     * 하위 서비스에 전달할 사용자 정보를 반환한다.
     *
     * 검증 내용:
     * 1. JWT 서명
     * 2. JWT 만료 시간
     * 3. tokenType ACCESS 여부
     * 4. 회원 ID 존재 여부
     * 5. 역할 존재 여부
     * 6. 닉네임 존재 여부
     */
    public JwtClaims validateAccessToken(
            String token
    ) {
        validateTokenValue(token);

        try {
            Claims claims = parseClaims(token);

            validateAccessTokenType(claims);

            Long userId =
                    extractUserId(claims);

            String role =
                    extractRole(claims);

            String nickname =
                    extractNickname(claims);

            return new JwtClaims(
                    userId,
                    role,
                    nickname
            );

        } catch (InvalidJwtException exception) {
            throw exception;

        } catch (ExpiredJwtException exception) {
            throw new InvalidJwtException(
                    "만료된 Access Token입니다.",
                    exception
            );

        } catch (JwtException exception) {
            throw new InvalidJwtException(
                    "유효하지 않은 Access Token입니다.",
                    exception
            );

        } catch (IllegalArgumentException exception) {
            throw new InvalidJwtException(
                    "Access Token 형식이 올바르지 않습니다.",
                    exception
            );
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private void validateAccessTokenType(
            Claims claims
    ) {
        String tokenType = claims.get(
                TOKEN_TYPE_CLAIM,
                String.class
        );

        if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
            throw new InvalidJwtException(
                    "Access Token이 아닙니다."
            );
        }
    }

    private Long extractUserId(Claims claims) {
        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new InvalidJwtException(
                    "토큰에 회원 ID가 없습니다."
            );
        }

        try {
            return Long.valueOf(subject);

        } catch (NumberFormatException exception) {
            throw new InvalidJwtException(
                    "토큰의 회원 ID 형식이 올바르지 않습니다.",
                    exception
            );
        }
    }

    private String extractRole(Claims claims) {
        String role = claims.get(
                ROLE_CLAIM,
                String.class
        );

        if (role == null || role.isBlank()) {
            throw new InvalidJwtException(
                    "토큰에 회원 역할이 없습니다."
            );
        }

        return role;
    }

    private String extractNickname(
            Claims claims
    ) {
        String nickname = claims.get(
                NICKNAME_CLAIM,
                String.class
        );

        if (
                nickname == null
                        || nickname.isBlank()
        ) {
            throw new InvalidJwtException(
                    "토큰에 회원 닉네임이 없습니다."
            );
        }

        return nickname;
    }

    private void validateTokenValue(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtException(
                    "Access Token이 비어 있습니다."
            );
        }
    }

    private void validateSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT Secret 설정이 필요합니다."
            );
        }

        byte[] secretBytes =
                secret.getBytes(StandardCharsets.UTF_8);

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT Secret은 32바이트 이상이어야 합니다."
            );
        }
    }
}