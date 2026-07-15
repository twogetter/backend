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

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";

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
     * Access Token을 검증하고,
     * 하위 서비스에 전달할 사용자 정보를 반환합니다.
     *
     * 검증 내용:
     * 1. JWT 서명이 올바른지
     * 2. JWT가 만료되지 않았는지
     * 3. tokenType이 ACCESS인지
     * 4. subject에 회원 ID가 존재하는지
     * 5. role Claim이 존재하는지
     */
    public JwtClaims validateAccessToken(String token) {
        validateTokenValue(token);

        try {
            Claims claims = parseClaims(token);

            validateAccessTokenType(claims);

            Long userId = extractUserId(claims);
            String role = extractRole(claims);

            return new JwtClaims(
                    userId,
                    role
            );

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

    private void validateAccessTokenType(Claims claims) {
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