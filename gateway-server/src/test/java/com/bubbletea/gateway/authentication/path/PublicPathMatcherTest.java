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

    @Test
    @DisplayName("POST 회원가입 경로는 공개 요청으로 처리한다")
    void signupPathIsPublic() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .post("/api/auth/signup")
                                .build()
                );

        assertTrue(
                publicPathMatcher.isPublic(exchange)
        );
    }

    @Test
    @DisplayName("POST 로그인 경로는 공개 요청으로 처리한다")
    void loginPathIsPublic() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .post("/api/auth/login")
                                .build()
                );

        assertTrue(
                publicPathMatcher.isPublic(exchange)
        );
    }

    @Test
    @DisplayName("공개 경로와 URL이 같아도 HTTP Method가 다르면 보호 요청이다")
    void publicPathWithDifferentMethodIsNotPublic() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/auth/login")
                                .build()
                );

        assertFalse(
                publicPathMatcher.isPublic(exchange)
        );
    }

    @Test
    @DisplayName("공개 경로와 비슷하지만 다른 경로는 보호 요청이다")
    void similarButDifferentPathIsNotPublic() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .post("/api/auth/login-fake")
                                .build()
                );

        assertFalse(
                publicPathMatcher.isPublic(exchange)
        );
    }

    @Test
    @DisplayName("일반 회원 API는 보호 요청으로 처리한다")
    void protectedMemberPathIsNotPublic() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/api/members/1")
                                .build()
                );

        assertFalse(
                publicPathMatcher.isPublic(exchange)
        );
    }
}