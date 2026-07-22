package com.bubbletea.gateway.authentication.path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicPathMatcherTest {

    private final PublicPathMatcher publicPathMatcher =
            new PublicPathMatcher();

    @Test
    @DisplayName("실제 CORS preflight 요청은 공개 요청으로 처리한다")
    void corsPreflightRequestIsPublic() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .options("/api/users/me")
                                .header(
                                        HttpHeaders.ORIGIN,
                                        "https://example.com"
                                )
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                        HttpMethod.GET.name()
                                )
                                .build()
                );

        // when
        boolean result =
                publicPathMatcher.isPublic(exchange);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("일반 OPTIONS 요청은 공개 요청으로 처리하지 않는다")
    void normalOptionsRequestIsNotPublic() {
        // given
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .options("/api/users/me")
                                .build()
                );

        // when
        boolean result =
                publicPathMatcher.isPublic(exchange);

        // then
        assertFalse(result);
    }
}