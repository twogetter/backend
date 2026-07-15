package com.bubbletea.gateway.authentication.filter;

import com.bubbletea.gateway.authentication.jwt.GatewayJwtProvider;
import com.bubbletea.gateway.authentication.jwt.InvalidJwtException;
import com.bubbletea.gateway.authentication.jwt.JwtClaims;
import com.bubbletea.gateway.authentication.path.PublicPathMatcher;
import com.bubbletea.gateway.authentication.support.AuthenticationErrorResponse;
import com.bubbletea.gateway.authentication.support.AuthenticationHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter
        implements GlobalFilter, Ordered {

    private static final String TOKEN_MISSING_CODE =
            "AUTH_TOKEN_MISSING";

    private static final String TOKEN_FORMAT_INVALID_CODE =
            "AUTH_TOKEN_FORMAT_INVALID";

    private static final String TOKEN_INVALID_CODE =
            "AUTH_TOKEN_INVALID";

    private final PublicPathMatcher publicPathMatcher;
    private final GatewayJwtProvider gatewayJwtProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            PublicPathMatcher publicPathMatcher,
            GatewayJwtProvider gatewayJwtProvider,
            ObjectMapper objectMapper
    ) {
        this.publicPathMatcher = publicPathMatcher;
        this.gatewayJwtProvider = gatewayJwtProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        /*
         * 클라이언트가 임의로 보낸 내부 인증 헤더를 제거합니다.
         *
         * 예:
         * X-User-Id: 999
         * X-User-Role: ADMIN
         */
        ServerWebExchange sanitizedExchange =
                removeClientAuthenticationHeaders(exchange);

        /*
         * 로그인, 회원가입, 토큰 재발급 등의 공개 API는
         * Access Token 검증 없이 통과합니다.
         */
        if (publicPathMatcher.isPublic(sanitizedExchange)) {
            return chain.filter(sanitizedExchange);
        }

        String authorizationHeader =
                sanitizedExchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.AUTHORIZATION
                        );

        if (
                authorizationHeader == null
                        || authorizationHeader.isBlank()
        ) {
            return writeUnauthorizedResponse(
                    sanitizedExchange,
                    TOKEN_MISSING_CODE,
                    "Access Token이 필요합니다."
            );
        }

        if (
                !authorizationHeader.startsWith(
                        AuthenticationHeaders.BEARER_PREFIX
                )
        ) {
            return writeUnauthorizedResponse(
                    sanitizedExchange,
                    TOKEN_FORMAT_INVALID_CODE,
                    "Authorization 헤더는 Bearer 형식이어야 합니다."
            );
        }

        String accessToken =
                authorizationHeader.substring(
                        AuthenticationHeaders.BEARER_PREFIX.length()
                ).trim();

        if (accessToken.isBlank()) {
            return writeUnauthorizedResponse(
                    sanitizedExchange,
                    TOKEN_MISSING_CODE,
                    "Access Token이 비어 있습니다."
            );
        }

        try {
            JwtClaims jwtClaims =
                    gatewayJwtProvider.validateAccessToken(
                            accessToken
                    );

            ServerWebExchange authenticatedExchange =
                    addAuthenticationHeaders(
                            sanitizedExchange,
                            jwtClaims
                    );

            return chain.filter(authenticatedExchange);

        } catch (InvalidJwtException exception) {
            return writeUnauthorizedResponse(
                    sanitizedExchange,
                    TOKEN_INVALID_CODE,
                    exception.getMessage()
            );
        }
    }

    /**
     * 클라이언트가 직접 보낸 내부 인증 헤더를 제거합니다.
     */
    private ServerWebExchange removeClientAuthenticationHeaders(
            ServerWebExchange exchange
    ) {
        return exchange.mutate()
                .request(requestBuilder ->
                        requestBuilder.headers(headers -> {
                            headers.remove(
                                    AuthenticationHeaders.USER_ID
                            );

                            headers.remove(
                                    AuthenticationHeaders.USER_ROLE
                            );
                        })
                )
                .build();
    }

    /**
     * JWT 검증을 통해 얻은 회원 ID와 역할을
     * 내부 인증 헤더에 추가합니다.
     */
    private ServerWebExchange addAuthenticationHeaders(
            ServerWebExchange exchange,
            JwtClaims jwtClaims
    ) {
        return exchange.mutate()
                .request(requestBuilder ->
                        requestBuilder.headers(headers -> {
                            /*
                             * 하위 서비스는 Gateway가 주입한 내부 헤더를
                             * 사용하도록 하고 원본 Access Token은 제거합니다.
                             */
                            headers.remove(
                                    AuthenticationHeaders.AUTHORIZATION
                            );

                            headers.set(
                                    AuthenticationHeaders.USER_ID,
                                    String.valueOf(
                                            jwtClaims.userId()
                                    )
                            );

                            headers.set(
                                    AuthenticationHeaders.USER_ROLE,
                                    jwtClaims.role()
                            );
                        })
                )
                .build();
    }

    /**
     * 인증 실패 응답을 JSON으로 반환합니다.
     */
    private Mono<Void> writeUnauthorizedResponse(
            ServerWebExchange exchange,
            String code,
            String message
    ) {
        if (exchange.getResponse().isCommitted()) {
            return exchange.getResponse().setComplete();
        }

        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        AuthenticationErrorResponse errorResponse =
                new AuthenticationErrorResponse(
                        code,
                        message
                );

        byte[] responseBody =
                serializeErrorResponse(errorResponse);

        DataBuffer dataBuffer =
                exchange.getResponse()
                        .bufferFactory()
                        .wrap(responseBody);

        return exchange.getResponse()
                .writeWith(Mono.just(dataBuffer));
    }

    /**
     * 인증 오류 객체를 JSON 바이트 배열로 변환합니다.
     */
    private byte[] serializeErrorResponse(
            AuthenticationErrorResponse errorResponse
    ) {
        try {
            return objectMapper.writeValueAsBytes(
                    errorResponse
            );

        } catch (JsonProcessingException exception) {
            String fallbackResponse = """
                    {
                      "code": "AUTHENTICATION_ERROR",
                      "message": "인증 처리 중 오류가 발생했습니다."
                    }
                    """;

            return fallbackResponse.getBytes(
                    StandardCharsets.UTF_8
            );
        }
    }

    /**
     * 다른 Gateway 필터보다 먼저 실행되도록 합니다.
     */
    @Override
    public int getOrder() {
        return -100;
    }
}