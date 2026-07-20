package com.bubbletea.gateway.authorization.filter;

import com.bubbletea.gateway.authentication.support.AuthenticationErrorResponse;
import com.bubbletea.gateway.authentication.support.AuthenticationHeaders;
import com.bubbletea.gateway.authorization.path.RolePathMatcher;
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
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class RoleAuthorizationFilter
        implements GlobalFilter, Ordered {

    private static final String FORBIDDEN_CODE =
            "AUTH_FORBIDDEN";

    private final RolePathMatcher rolePathMatcher;
    private final ObjectMapper objectMapper;

    public RoleAuthorizationFilter(
            RolePathMatcher rolePathMatcher,
            ObjectMapper objectMapper
    ) {
        this.rolePathMatcher = rolePathMatcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        /*
         * 현재 요청 경로에 설정된 역할 규칙을 조회합니다.
         *
         * 역할 제한이 없는 일반 API라면
         * 별도의 인가 검사 없이 요청을 통과시킵니다.
         */
        Optional<Set<String>> allowedRolesOptional =
                rolePathMatcher.findAllowedRoles(exchange);

        if (allowedRolesOptional.isEmpty()) {
            return chain.filter(exchange);
        }

        /*
         * JwtAuthenticationFilter가 검증한 JWT의 역할을
         * X-User-Role 내부 헤더에서 조회합니다.
         */
        String userRole =
                exchange.getRequest()
                        .getHeaders()
                        .getFirst(
                                AuthenticationHeaders.USER_ROLE
                        );

        Set<String> allowedRoles =
                allowedRolesOptional.get();

        /*
         * 사용자 역할이 존재하고, 요청 경로에서 허용하는
         * 역할 목록에 포함되어 있으면 요청을 통과시킵니다.
         */
        if (
                userRole != null
                        && allowedRoles.contains(
                        normalizeRole(userRole)
                )
        ) {
            return chain.filter(exchange);
        }

        return writeForbiddenResponse(
                exchange,
                FORBIDDEN_CODE,
                "해당 API에 접근할 권한이 없습니다."
        );
    }

    /**
     * JWT 또는 내부 헤더의 역할 값을
     * 비교하기 좋은 대문자 형식으로 정규화합니다.
     */
    private String normalizeRole(
            String userRole
    ) {
        return userRole.trim()
                .toUpperCase(Locale.ROOT);
    }

    /**
     * 역할이 부족한 경우 403 Forbidden 응답을 반환합니다.
     */
    private Mono<Void> writeForbiddenResponse(
            ServerWebExchange exchange,
            String code,
            String message
    ) {
        if (exchange.getResponse().isCommitted()) {
            return exchange.getResponse().setComplete();
        }

        exchange.getResponse()
                .setStatusCode(HttpStatus.FORBIDDEN);

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
     * 인가 실패 응답을 JSON 바이트 배열로 변환합니다.
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
                      "code": "AUTHORIZATION_ERROR",
                      "message": "권한 확인 중 오류가 발생했습니다."
                    }
                    """;

            return fallbackResponse.getBytes(
                    StandardCharsets.UTF_8
            );
        }
    }

    /**
     * JwtAuthenticationFilter의 실행 순서는 -100입니다.
     *
     * JWT 인증 필터에서 X-User-Role 헤더를 추가한 뒤
     * 역할 인가 검사를 수행하도록 -90으로 지정합니다.
     */
    @Override
    public int getOrder() {
        return -90;
    }
}