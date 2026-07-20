package com.bubbletea.gateway.authentication.path;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

@Component
public class PublicPathMatcher {

    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS =
            List.of(
                    new PublicEndpoint(
                            HttpMethod.POST,
                            "/api/auth/signup"
                    ),
                    new PublicEndpoint(
                            HttpMethod.POST,
                            "/api/auth/login"
                    ),
                    new PublicEndpoint(
                            HttpMethod.POST,
                            "/api/auth/refresh"
                    ),
                    new PublicEndpoint(
                            HttpMethod.POST,
                            "/api/auth/logout"
                    ),
                    new PublicEndpoint(
                            HttpMethod.GET,
                            "/actuator/health"
                    ),
                    new PublicEndpoint(
                            HttpMethod.GET,
                            "/actuator/health/**"
                    ),
                    new PublicEndpoint(
                            HttpMethod.GET,
                            "/swagger-ui/**"
                    ),
                    new PublicEndpoint(
                            HttpMethod.GET,
                            "/v3/api-docs/**"
                    )
            );

    private final AntPathMatcher pathMatcher =
            new AntPathMatcher();

    /**
     * 현재 요청이 JWT 인증을 요구하지 않는
     * 공개 API인지 확인합니다.
     */
    public boolean isPublic(
            ServerWebExchange exchange
    ) {
        HttpMethod requestMethod =
                exchange.getRequest()
                        .getMethod();

        String requestPath =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        /*
         * Origin과 Access-Control-Request-Method 헤더가 포함된
         * 실제 CORS 사전 요청만 Access Token 없이 통과시킵니다.
         *
         * 단순한 OPTIONS 요청은 공개 요청으로 처리하지 않습니다.
         */
        if (
                CorsUtils.isPreFlightRequest(
                        exchange.getRequest()
                )
        ) {
            return true;
        }

        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(endpoint ->
                        endpoint.matches(
                                requestMethod,
                                requestPath,
                                pathMatcher
                        )
                );
    }

    /**
     * 공개 API의 HTTP Method와 경로를 함께 관리합니다.
     *
     * 경로만 비교하면 GET /api/auth/login 같은 잘못된 요청도
     * 공개 처리될 수 있으므로 HTTP Method까지 같이 확인합니다.
     */
    private record PublicEndpoint(
            HttpMethod method,
            String pathPattern
    ) {

        private boolean matches(
                HttpMethod requestMethod,
                String requestPath,
                AntPathMatcher pathMatcher
        ) {
            return method.equals(requestMethod)
                    && pathMatcher.match(
                    pathPattern,
                    requestPath
            );
        }
    }
}