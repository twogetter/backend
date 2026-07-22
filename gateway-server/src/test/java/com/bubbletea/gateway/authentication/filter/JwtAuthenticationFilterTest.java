package com.bubbletea.gateway.authentication.filter;

import com.bubbletea.gateway.authentication.jwt.GatewayJwtProvider;
import com.bubbletea.gateway.authentication.path.PublicPathMatcher;
import com.bubbletea.gateway.authentication.support.AuthenticationHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "bubbletea-gateway-filter-test-secret-key-must-be-longer-than-32-bytes";

    private static final String ROLE_CLAIM =
            "role";

    private static final String NICKNAME_CLAIM =
            "nickname";

    private static final String TOKEN_TYPE_CLAIM =
            "tokenType";

    @Mock
    private GatewayFilterChain gatewayFilterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        PublicPathMatcher publicPathMatcher =
                new PublicPathMatcher();

        GatewayJwtProvider gatewayJwtProvider =
                new GatewayJwtProvider(SECRET);

        ObjectMapper objectMapper =
                new ObjectMapper();

        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        publicPathMatcher,
                        gatewayJwtProvider,
                        objectMapper
                );

        secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("로그인 API는 Access Token 없이 통과한다")
    void publicLoginApiPassesWithoutToken() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .post("/api/auth/login")
                                .build()
                );

        when(
                gatewayFilterChain.filter(
                        any(ServerWebExchange.class)
                )
        ).thenReturn(Mono.empty());

        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        StepVerifier.create(result)
                .verifyComplete();

        verify(gatewayFilterChain)
                .filter(
                        any(ServerWebExchange.class)
                );

        assertNull(
                exchange.getResponse().getStatusCode()
        );
    }

    @Test
    @DisplayName("공개 API에서도 위조된 내부 인증 헤더를 제거한다")
    void publicApiRemovesForgedAuthenticationHeaders() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .post("/api/auth/login")
                                .header(
                                        AuthenticationHeaders.USER_ID,
                                        "999"
                                )
                                .header(
                                        AuthenticationHeaders.USER_ROLE,
                                        "ADMIN"
                                )
                                .header(
                                        AuthenticationHeaders.USER_NICKNAME,
                                        "forged"
                                )
                                .build()
                );

        when(
                gatewayFilterChain.filter(
                        any(ServerWebExchange.class)
                )
        ).thenReturn(Mono.empty());

        ArgumentCaptor<ServerWebExchange> captor =
                ArgumentCaptor.forClass(
                        ServerWebExchange.class
                );

        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        StepVerifier.create(result)
                .verifyComplete();

        verify(gatewayFilterChain)
                .filter(captor.capture());

        ServerWebExchange forwardedExchange =
                captor.getValue();

        assertNull(
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_ID
                        )
        );

        assertNull(
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_ROLE
                        )
        );

        assertNull(
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_NICKNAME
                        )
        );
    }

    @Test
    @DisplayName("보호 API에 토큰이 없으면 401을 반환한다")
    void protectedApiWithoutTokenReturnsUnauthorized() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/users/me")
                                .build()
                );

        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        String responseBody =
                exchange.getResponse()
                        .getBodyAsString()
                        .block();

        assertTrue(
                responseBody.contains(
                        "AUTH_TOKEN_MISSING"
                )
        );

        verify(
                gatewayFilterChain,
                never()
        ).filter(
                any(ServerWebExchange.class)
        );
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 401을 반환한다")
    void invalidBearerFormatReturnsUnauthorized() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "invalid-token"
                                )
                                .build()
                );

        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        String responseBody =
                exchange.getResponse()
                        .getBodyAsString()
                        .block();

        assertTrue(
                responseBody.contains(
                        "AUTH_TOKEN_FORMAT_INVALID"
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Bearer",
            "bearer",
            "BEARER",
            "BeArEr"
    })
    @DisplayName("Bearer 인증 스킴은 대소문자를 구분하지 않는다")
    void bearerSchemeIsCaseInsensitive(
            String bearerScheme
    ) {
        String nickname = "테스터";

        String accessToken = createToken(
                1L,
                "USER",
                nickname,
                "ACCESS"
        );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearerScheme
                                                + " "
                                                + accessToken
                                )
                                .build()
                );

        when(
                gatewayFilterChain.filter(
                        any(ServerWebExchange.class)
                )
        ).thenReturn(Mono.empty());

        ArgumentCaptor<ServerWebExchange> captor =
                ArgumentCaptor.forClass(
                        ServerWebExchange.class
                );

        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        StepVerifier.create(result)
                .verifyComplete();

        verify(gatewayFilterChain)
                .filter(captor.capture());

        ServerWebExchange forwardedExchange =
                captor.getValue();

        assertEquals(
                "1",
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_ID
                        )
        );

        assertEquals(
                "USER",
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_ROLE
                        )
        );

        assertEquals(
                encodeNickname(nickname),
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_NICKNAME
                        )
        );

        assertNull(
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.AUTHORIZATION
                        )
        );
    }

    @Test
    @DisplayName("클라이언트가 위조한 인증 헤더는 JWT 값으로 교체한다")
    void forgedAuthenticationHeadersAreReplaced() {
        String nickname = "실제닉네임";

        String accessToken = createToken(
                7L,
                "USER",
                nickname,
                "ACCESS"
        );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .header(
                                        AuthenticationHeaders.USER_ID,
                                        "999"
                                )
                                .header(
                                        AuthenticationHeaders.USER_ROLE,
                                        "ADMIN"
                                )
                                .header(
                                        AuthenticationHeaders.USER_NICKNAME,
                                        "forged"
                                )
                                .build()
                );

        when(
                gatewayFilterChain.filter(
                        any(ServerWebExchange.class)
                )
        ).thenReturn(Mono.empty());

        ArgumentCaptor<ServerWebExchange> captor =
                ArgumentCaptor.forClass(
                        ServerWebExchange.class
                );

        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        StepVerifier.create(result)
                .verifyComplete();

        verify(gatewayFilterChain)
                .filter(captor.capture());

        ServerWebExchange forwardedExchange =
                captor.getValue();

        String forwardedUserId =
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_ID
                        );

        String forwardedRole =
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_ROLE
                        );

        String forwardedNickname =
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_NICKNAME
                        );

        assertEquals("7", forwardedUserId);
        assertEquals("USER", forwardedRole);

        assertEquals(
                encodeNickname(nickname),
                forwardedNickname
        );

        assertFalse(
                "999".equals(forwardedUserId)
        );

        assertFalse(
                "ADMIN".equals(forwardedRole)
        );

        assertFalse(
                "forged".equals(forwardedNickname)
        );
    }

    @Test
    @DisplayName("nickname 클레임이 없으면 401을 반환한다")
    void missingNicknameClaimReturnsUnauthorized() {
        String accessToken = createToken(
                1L,
                "USER",
                null,
                "ACCESS"
        );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .build()
                );

        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        String responseBody =
                exchange.getResponse()
                        .getBodyAsString()
                        .block();

        assertTrue(
                responseBody.contains(
                        "토큰에 회원 닉네임이 없습니다."
                )
        );

        verify(
                gatewayFilterChain,
                never()
        ).filter(
                any(ServerWebExchange.class)
        );
    }

    @Test
    @DisplayName("Refresh Token으로 보호 API에 접근하면 401을 반환한다")
    void refreshTokenReturnsUnauthorized() {
        String refreshToken = createToken(
                1L,
                "USER",
                null,
                "REFRESH"
        );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + refreshToken
                                )
                                .build()
                );

        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        String responseBody =
                exchange.getResponse()
                        .getBodyAsString()
                        .block();

        assertTrue(
                responseBody.contains(
                        "Access Token이 아닙니다."
                )
        );

        verify(
                gatewayFilterChain,
                never()
        ).filter(
                any(ServerWebExchange.class)
        );
    }

    private String createToken(
            Long memberId,
            String role,
            String nickname,
            String tokenType
    ) {
        Instant now = Instant.now();

        JwtBuilder builder =
                Jwts.builder()
                        .subject(
                                String.valueOf(memberId)
                        )
                        .claim(
                                ROLE_CLAIM,
                                role
                        )
                        .claim(
                                TOKEN_TYPE_CLAIM,
                                tokenType
                        )
                        .issuedAt(
                                Date.from(now)
                        )
                        .expiration(
                                Date.from(
                                        now.plusSeconds(3600)
                                )
                        );

        if (nickname != null) {
            builder.claim(
                    NICKNAME_CLAIM,
                    nickname
            );
        }

        return builder
                .signWith(secretKey)
                .compact();
    }

    private String encodeNickname(
            String nickname
    ) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        nickname.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }
}