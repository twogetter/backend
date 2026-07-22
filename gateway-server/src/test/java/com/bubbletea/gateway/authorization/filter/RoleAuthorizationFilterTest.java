package com.bubbletea.gateway.authorization.filter;

import com.bubbletea.gateway.authentication.support.AuthenticationHeaders;
import com.bubbletea.gateway.authorization.path.RolePathMatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleAuthorizationFilterTest {

    @Mock
    private GatewayFilterChain gatewayFilterChain;

    private RoleAuthorizationFilter roleAuthorizationFilter;

    @BeforeEach
    void setUp() {
        RolePathMatcher rolePathMatcher =
                new RolePathMatcher();

        ObjectMapper objectMapper =
                new ObjectMapper();

        roleAuthorizationFilter =
                new RoleAuthorizationFilter(
                        rolePathMatcher,
                        objectMapper
                );
    }

    @Test
    @DisplayName("ADMIN 역할은 관리자 API에 접근할 수 있다")
    void adminCanAccessAdminApi() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/admin/users")
                                .header(
                                        AuthenticationHeaders.USER_ID,
                                        "1"
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

        // when
        Mono<Void> result =
                roleAuthorizationFilter.filter(
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
    @DisplayName("USER 역할은 관리자 API에 접근할 수 없다")
    void userCannotAccessAdminApi() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/admin/users")
                                .header(
                                        AuthenticationHeaders.USER_ID,
                                        "2"
                                )
                                .header(
                                        AuthenticationHeaders.USER_ROLE,
                                        "USER"
                                )
                                .build()
                );

        // when
        Mono<Void> result =
                roleAuthorizationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        // then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(
                HttpStatus.FORBIDDEN,
                exchange.getResponse().getStatusCode()
        );

        String responseBody =
                exchange.getResponse()
                        .getBodyAsString()
                        .block();

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

        verify(
                gatewayFilterChain,
                never()
        ).filter(
                any(ServerWebExchange.class)
        );
    }

    @Test
    @DisplayName("역할 헤더가 없으면 관리자 API 접근을 거부한다")
    void requestWithoutRoleCannotAccessAdminApi() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/admin/users")
                                .build()
                );

        // when
        Mono<Void> result =
                roleAuthorizationFilter.filter(
                        exchange,
                        gatewayFilterChain
                );

        // then
        StepVerifier.create(result)
                .verifyComplete();

        assertEquals(
                HttpStatus.FORBIDDEN,
                exchange.getResponse().getStatusCode()
        );

        String responseBody =
                exchange.getResponse()
                        .getBodyAsString()
                        .block();

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

        verify(
                gatewayFilterChain,
                never()
        ).filter(
                any(ServerWebExchange.class)
        );
    }

    @Test
    @DisplayName("일반 API는 역할 제한 없이 통과한다")
    void normalApiPassesWithoutRoleRestriction() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/users/me")
                                .header(
                                        AuthenticationHeaders.USER_ID,
                                        "2"
                                )
                                .header(
                                        AuthenticationHeaders.USER_ROLE,
                                        "USER"
                                )
                                .build()
                );

        when(
                gatewayFilterChain.filter(
                        any(ServerWebExchange.class)
                )
        ).thenReturn(Mono.empty());

        // when
        Mono<Void> result =
                roleAuthorizationFilter.filter(
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
    @DisplayName("역할 비교 시 대소문자를 구분하지 않는다")
    void roleComparisonIsCaseInsensitive() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/admin/users")
                                .header(
                                        AuthenticationHeaders.USER_ID,
                                        "1"
                                )
                                .header(
                                        AuthenticationHeaders.USER_ROLE,
                                        "admin"
                                )
                                .build()
                );

        when(
                gatewayFilterChain.filter(
                        any(ServerWebExchange.class)
                )
        ).thenReturn(Mono.empty());

        // when
        Mono<Void> result =
                roleAuthorizationFilter.filter(
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
    @DisplayName("인가 필터는 JWT 인증 필터 다음에 실행된다")
    void authorizationFilterRunsAfterAuthenticationFilter() {
        // when
        int filterOrder =
                roleAuthorizationFilter.getOrder();

        // then
        assertEquals(
                -90,
                filterOrder
        );

        assertTrue(
                filterOrder > -100
        );
    }
}