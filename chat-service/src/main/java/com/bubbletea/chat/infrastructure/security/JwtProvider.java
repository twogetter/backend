package com.bubbletea.chat.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 토큰 서명 검증 및 클레임 파싱을 담당하는 컴포넌트입니다.
 */
@Component
public class JwtProvider {

  private static final String ROLE_CLAIM = "role";
  private static final String NICKNAME_CLAIM = "nickname";
  private static final String TOKEN_TYPE_CLAIM = "tokenType";

  private final SecretKey secretKey;

  public JwtProvider(JwtProperties jwtProperties) {
    if (jwtProperties == null || jwtProperties.secret() == null || jwtProperties.secret().isBlank()) {
      throw new IllegalStateException("jwt.secret 프로퍼티 설정이 누락되었거나 비어 있습니다.");
    }
    this.secretKey = Keys.hmacShaKeyFor(
        jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
    );
  }

  public boolean isValid(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
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
      throw new IllegalArgumentException("토큰의 회원 식별자가 올바르지 않습니다.", exception);
    }
  }

  public String getRole(String token) {
    return parseClaims(token).get(ROLE_CLAIM, String.class);
  }

  public String getNickname(String token) {
    return parseClaims(token).get(NICKNAME_CLAIM, String.class);
  }

  public void validateAccessToken(String token) {
    Claims claims = parseClaims(token);
    String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
    if (!TokenType.ACCESS.name().equals(tokenType)) {
      throw new IllegalArgumentException("Access Token이 아닙니다.");
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public enum TokenType {
    ACCESS, REFRESH
  }
}
