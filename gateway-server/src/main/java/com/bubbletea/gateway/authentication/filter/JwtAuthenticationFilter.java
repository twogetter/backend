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
import java.util.Base64;

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
         * 클라이언트가 임의로 보낸 내부 인증 헤더를
         * 공개 API와 보호 API 모두에서 제거한다.
         */
        ServerWebExchange sanitizedExchange =
                removeClientAuthenticationHeaders(exchange);

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

        if (!hasBearerPrefix(authorizationHeader)) {
            return writeUnauthorizedResponse(
                    sanitizedExchange,
                    TOKEN_FORMAT_INVALID_CODE,
                    "Authorization 헤더는 Bearer 형식이어야 합니다."
            );
        }

        String accessToken =
                authorizationHeader.substring(
                        AuthenticationHeaders
                                .BEARER_PREFIX
                                .length()
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

    private boolean hasBearerPrefix(
            String authorizationHeader
    ) {
        return authorizationHeader.regionMatches(
                true,
                0,
                AuthenticationHeaders.BEARER_PREFIX,
                0,
                AuthenticationHeaders
                        .BEARER_PREFIX
                        .length()
        );
    }

    /**
     * 클라이언트가 직접 전달한 내부 헤더를 제거한다.
     *
     * Gateway만 해당 헤더를 생성할 수 있다.
     */
    private ServerWebExchange
    removeClientAuthenticationHeaders(
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

                            headers.remove(
                                    AuthenticationHeaders.USER_NICKNAME
                            );
                        })
                )
                .build();
    }

    /**
     * 검증된 JWT 정보를 내부 헤더로 변환한다.
     *
     * 한글 닉네임을 HTTP 헤더에 직접 넣지 않고
     * UTF-8 Base64 URL 형식으로 인코딩한다.
     */
    private ServerWebExchange addAuthenticationHeaders(
            ServerWebExchange exchange,
            JwtClaims jwtClaims
    ) {
        return exchange.mutate()
                .request(requestBuilder ->
                        requestBuilder.headers(headers -> {
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

                            headers.set(
                                    AuthenticationHeaders.USER_NICKNAME,
                                    encodeNickname(
                                            jwtClaims.nickname()
                                    )
                            );
                        })
                )
                .build();
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
                .setContentType(
                        MediaType.APPLICATION_JSON
                );

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

    @Override
    public int getOrder() {
        return -100;
    }
}