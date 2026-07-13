package com.bubbletea.auth.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.secret()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    public String createAccessToken(
            Long memberId,
            String role
    ) {
        return createToken(
                memberId,
                role,
                TokenType.ACCESS,
                jwtProperties.accessTokenExpiration()
        );
    }

    public String createRefreshToken(
            Long memberId,
            String role
    ) {
        return createToken(
                memberId,
                role,
                TokenType.REFRESH,
                jwtProperties.refreshTokenExpiration()
        );
    }

    private String createToken(
            Long memberId,
            String role,
            TokenType tokenType,
            long expirationMillis
    ) {
        Instant now = Instant.now();
        Instant expiration =
                now.plusMillis(expirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(ROLE_CLAIM, role)
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public Long getMemberId(String token) {
        String subject = parseClaims(token).getSubject();

        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "토큰의 회원 식별자가 올바르지 않습니다.",
                    exception
            );
        }
    }

    public String getRole(String token) {
        return parseClaims(token).get(
                ROLE_CLAIM,
                String.class
        );
    }

    public TokenType getTokenType(String token) {
        String tokenType = parseClaims(token).get(
                TOKEN_TYPE_CLAIM,
                String.class
        );

        try {
            return TokenType.valueOf(tokenType);
        } catch (
                IllegalArgumentException |
                NullPointerException exception
        ) {
            throw new IllegalArgumentException(
                    "토큰 유형이 올바르지 않습니다.",
                    exception
            );
        }
    }

    public void validateRefreshToken(String token) {
        Claims claims = parseClaims(token);

        String tokenType = claims.get(
                TOKEN_TYPE_CLAIM,
                String.class
        );

        if (!TokenType.REFRESH.name().equals(tokenType)) {
            throw new IllegalArgumentException(
                    "Refresh Token이 아닙니다."
            );
        }
    }

    public Duration getRemainingExpiration(String token) {
        Date expiration =
                parseClaims(token).getExpiration();

        long remainingMillis =
                expiration.getTime()
                        - Instant.now().toEpochMilli();

        if (remainingMillis <= 0) {
            throw new IllegalArgumentException(
                    "이미 만료된 토큰입니다."
            );
        }

        return Duration.ofMillis(remainingMillis);
    }

    public long getAccessTokenExpirationSeconds() {
        return Duration.ofMillis(
                jwtProperties.accessTokenExpiration()
        ).toSeconds();
    }

    public long getRefreshTokenExpirationSeconds() {
        return Duration.ofMillis(
                jwtProperties.refreshTokenExpiration()
        ).toSeconds();
    }

    public Duration getRefreshTokenExpiration() {
        return Duration.ofMillis(
                jwtProperties.refreshTokenExpiration()
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}