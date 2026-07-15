package com.bubbletea.gateway.authentication.filter;

import com.bubbletea.gateway.authentication.jwt.GatewayJwtProvider;
import com.bubbletea.gateway.authentication.path.PublicPathMatcher;
import com.bubbletea.gateway.authentication.support.AuthenticationHeaders;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
        // given
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

        // when
        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        // then
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
    @DisplayName("공개 API에서도 클라이언트가 보낸 내부 인증 헤더를 제거한다")
    void publicApiRemovesForgedAuthenticationHeaders() {
        // given
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
                                .build()
                );

        when(
                gatewayFilterChain.filter(
                        any(ServerWebExchange.class)
                )
        ).thenReturn(Mono.empty());

        ArgumentCaptor<ServerWebExchange> exchangeCaptor =
                ArgumentCaptor.forClass(
                        ServerWebExchange.class
                );

        // when
        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(gatewayFilterChain)
                .filter(exchangeCaptor.capture());

        ServerWebExchange forwardedExchange =
                exchangeCaptor.getValue();

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
    }

    @Test
    @DisplayName("보호 API에 Authorization 헤더가 없으면 401을 반환한다")
    void protectedApiWithoutTokenReturnsUnauthorized() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/users/me")
                                .build()
                );

        // when
        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        // then
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

        assertTrue(
                responseBody.contains(
                        "Access Token이 필요합니다."
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
    @DisplayName("Bearer 형식이 아닌 Authorization 헤더는 401을 반환한다")
    void invalidBearerFormatReturnsUnauthorized() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "invalid-access-token"
                                )
                                .build()
                );

        // when
        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        // then
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

        assertTrue(
                responseBody.contains(
                        "Bearer"
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
    @DisplayName("정상 Access Token이면 회원 ID와 역할을 내부 헤더에 추가한다")
    void validAccessTokenAddsAuthenticationHeaders() {
        // given
        String accessToken = createToken(
                1L,
                "USER",
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

        when(
                gatewayFilterChain.filter(
                        any(ServerWebExchange.class)
                )
        ).thenReturn(Mono.empty());

        ArgumentCaptor<ServerWebExchange> exchangeCaptor =
                ArgumentCaptor.forClass(
                        ServerWebExchange.class
                );

        // when
        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(gatewayFilterChain)
                .filter(exchangeCaptor.capture());

        ServerWebExchange forwardedExchange =
                exchangeCaptor.getValue();

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

        assertNull(
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.AUTHORIZATION
                        )
        );
    }

    @Test
    @DisplayName("클라이언트가 위조한 인증 헤더는 JWT의 실제 값으로 교체한다")
    void forgedAuthenticationHeadersAreReplaced() {
        // given
        String accessToken = createToken(
                7L,
                "USER",
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
                                .build()
                );

        when(
                gatewayFilterChain.filter(
                        any(ServerWebExchange.class)
                )
        ).thenReturn(Mono.empty());

        ArgumentCaptor<ServerWebExchange> exchangeCaptor =
                ArgumentCaptor.forClass(
                        ServerWebExchange.class
                );

        // when
        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        // then
        StepVerifier.create(result)
                .verifyComplete();

        verify(gatewayFilterChain)
                .filter(exchangeCaptor.capture());

        ServerWebExchange forwardedExchange =
                exchangeCaptor.getValue();

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

        assertEquals("7", forwardedUserId);
        assertEquals("USER", forwardedRole);

        assertFalse(
                "999".equals(forwardedUserId)
        );

        assertFalse(
                "ADMIN".equals(forwardedRole)
        );
    }

    @Test
    @DisplayName("Refresh Token으로 보호 API에 접근하면 401을 반환한다")
    void refreshTokenReturnsUnauthorized() {
        // given
        String refreshToken = createToken(
                1L,
                "USER",
                "REFRESH"
        );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .method(
                                        HttpMethod.GET,
                                        "/api/users/me"
                                )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + refreshToken
                                )
                                .build()
                );

        // when
        Mono<Void> result =
                jwtAuthenticationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        // then
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
                        "AUTH_TOKEN_INVALID"
                )
        );

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
            String tokenType
    ) {
        Instant now = Instant.now();

        return Jwts.builder()
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
                )
                .signWith(secretKey)
                .compact();
    }
}