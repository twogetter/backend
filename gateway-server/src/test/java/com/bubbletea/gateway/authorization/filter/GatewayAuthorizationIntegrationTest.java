package com.bubbletea.gateway.authorization.filter;

import com.bubbletea.gateway.authentication.filter.JwtAuthenticationFilter;
import com.bubbletea.gateway.authentication.jwt.GatewayJwtProvider;
import com.bubbletea.gateway.authentication.path.PublicPathMatcher;
import com.bubbletea.gateway.authentication.support.AuthenticationHeaders;
import com.bubbletea.gateway.authorization.path.RolePathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAuthorizationIntegrationTest {

    private static final String SECRET =
            "bubbletea-gateway-authorization-integration-test-secret-key-must-be-longer-than-32-bytes";

    private static final String ROLE_CLAIM =
            "role";

    private static final String TOKEN_TYPE_CLAIM =
            "tokenType";

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private RoleAuthorizationFilter roleAuthorizationFilter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper =
                new ObjectMapper();

        GatewayJwtProvider gatewayJwtProvider =
                new GatewayJwtProvider(SECRET);

        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        new PublicPathMatcher(),
                        gatewayJwtProvider,
                        objectMapper
                );

        roleAuthorizationFilter =
                new RoleAuthorizationFilter(
                        new RolePathMatcher(),
                        objectMapper
                );

        secretKey =
                Keys.hmacShaKeyFor(
                        SECRET.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }

    @Test
    @DisplayName(
            "ADMIN Access Token으로 관리자 API를 요청하면 "
                    + "인증과 인가를 통과한다"
    )
    void adminAccessTokenPassesAdminApi() {
        // given
        String accessToken =
                createAccessToken(
                        1L,
                        "ADMIN"
                );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/admin/users")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .build()
                );

        AtomicReference<ServerWebExchange>
                terminalExchangeReference =
                new AtomicReference<>();

        // when
        Mono<Void> result =
                executeFilterChain(
                        exchange,
                        terminalExchangeReference
                );

        // then
        StepVerifier.create(result)
                .verifyComplete();

        /*
         * 정상 요청이므로 Gateway의 응답 상태는
         * 별도로 설정되지 않습니다.
         */
        assertNull(
                exchange.getResponse()
                        .getStatusCode()
        );

        ServerWebExchange forwardedExchange =
                terminalExchangeReference.get();

        assertNotNull(forwardedExchange);

        assertEquals(
                "1",
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_ID
                        )
        );

        assertEquals(
                "ADMIN",
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_ROLE
                        )
        );

        /*
         * JWT 인증이 끝난 뒤 원본 Authorization 헤더는
         * 하위 서비스로 전달하지 않습니다.
         */
        assertNull(
                forwardedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.AUTHORIZATION
                        )
        );
    }

    @Test
    @DisplayName(
            "USER Access Token으로 관리자 API를 요청하면 "
                    + "403을 반환한다"
    )
    void userAccessTokenCannotAccessAdminApi() {
        // given
        String accessToken =
                createAccessToken(
                        2L,
                        "USER"
                );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/admin/users")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + accessToken
                                )
                                .build()
                );

        AtomicReference<ServerWebExchange>
                terminalExchangeReference =
                new AtomicReference<>();

        // when
        Mono<Void> result =
                executeFilterChain(
                        exchange,
                        terminalExchangeReference
                );

        // then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(
                HttpStatus.FORBIDDEN,
                exchange.getResponse()
                        .getStatusCode()
        );

        String responseBody =
                exchange.getResponse()
                        .getBodyAsString()
                        .block();

        assertNotNull(responseBody);

        assertTrue(
                responseBody.contains(
                        "AUTH_FORBIDDEN"
                )
        );

        assertTrue(
                responseBody.contains(
                        "해당 API에 접근할 권한이 없습니다."
                )
        );

        /*
         * 인가 단계에서 차단되었으므로
         * 최종 하위 서비스 체인까지 전달되지 않습니다.
         */
        assertNull(
                terminalExchangeReference.get()
        );
    }

    @Test
    @DisplayName(
            "Access Token 없이 관리자 API를 요청하면 "
                    + "인가 검사 전에 401을 반환한다"
    )
    void requestWithoutAccessTokenReturnsUnauthorized() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/admin/users")
                                .build()
                );

        AtomicReference<ServerWebExchange>
                terminalExchangeReference =
                new AtomicReference<>();

        // when
        Mono<Void> result =
                executeFilterChain(
                        exchange,
                        terminalExchangeReference
                );

        // then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse()
                        .getStatusCode()
        );

        String responseBody =
                exchange.getResponse()
                        .getBodyAsString()
                        .block();

        assertNotNull(responseBody);

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

        /*
         * JWT 인증 단계에서 차단되었으므로
         * 인가 이후의 최종 체인까지 전달되지 않습니다.
         */
        assertNull(
                terminalExchangeReference.get()
        );
    }

    @Test
    @DisplayName(
            "클라이언트가 ADMIN 역할 헤더를 위조해도 "
                    + "USER 토큰이면 관리자 API 접근을 차단한다"
    )
    void forgedAdminHeaderCannotBypassAuthorization() {
        // given
        String userAccessToken =
                createAccessToken(
                        7L,
                        "USER"
                );

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/admin/users")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + userAccessToken
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

        AtomicReference<ServerWebExchange>
                terminalExchangeReference =
                new AtomicReference<>();

        // when
        Mono<Void> result =
                executeFilterChain(
                        exchange,
                        terminalExchangeReference
                );

        // then
        StepVerifier.create(result)
                .verifyComplete();

        /*
         * 클라이언트가 보낸 ADMIN 헤더는 제거되고,
         * JWT의 실제 역할인 USER로 교체됩니다.
         *
         * 따라서 관리자 API 접근은 거부되어야 합니다.
         */
        assertEquals(
                HttpStatus.FORBIDDEN,
                exchange.getResponse()
                        .getStatusCode()
        );

        String responseBody =
                exchange.getResponse()
                        .getBodyAsString()
                        .block();

        assertNotNull(responseBody);

        assertTrue(
                responseBody.contains(
                        "AUTH_FORBIDDEN"
                )
        );

        assertNull(
                terminalExchangeReference.get()
        );
    }

    /**
     * 실제 Gateway 필터 실행 순서와 동일하게
     * JWT 인증 필터 다음에 역할 인가 필터를 연결합니다.
     */
    private Mono<Void> executeFilterChain(
            ServerWebExchange exchange,
            AtomicReference<ServerWebExchange>
                    terminalExchangeReference
    ) {
        /*
         * 모든 필터를 통과했을 때 실행되는
         * 최종 GatewayFilterChain입니다.
         *
         * 실제 테스트에서는 하위 서비스로 전달된 요청을
         * AtomicReference에 저장합니다.
         */
        GatewayFilterChain terminalChain =
                forwardedExchange -> {
                    terminalExchangeReference.set(
                            forwardedExchange
                    );

                    return Mono.empty();
                };

        /*
         * JWT 인증 필터가 인증을 완료하면
         * 역할 인가 필터를 실행하도록 연결합니다.
         */
        GatewayFilterChain authorizationChain =
                authenticatedExchange ->
                        roleAuthorizationFilter.filter(
                                authenticatedExchange,
                                terminalChain
                        );

        return jwtAuthenticationFilter.filter(
                exchange,
                authorizationChain
        );
    }

    /**
     * GatewayJwtProvider가 검증할 수 있는
     * 테스트용 Access Token을 생성합니다.
     */
    private String createAccessToken(
            Long memberId,
            String role
    ) {
        Instant now =
                Instant.now();

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
                        "ACCESS"
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