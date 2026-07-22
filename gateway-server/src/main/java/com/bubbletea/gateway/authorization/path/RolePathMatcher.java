package com.bubbletea.gateway.authorization.path;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class RolePathMatcher {

    /*
     * Gateway에서 공통으로 제한할 역할별 경로를 정의합니다.
     *
     * 현재 1차 구현에서는 /api/admin/** 경로를
     * ADMIN 역할만 접근할 수 있도록 제한합니다.
     */
    private static final List<RoleRule> ROLE_RULES =
            List.of(
                    new RoleRule(
                            "/api/admin/**",
                            Set.of("ADMIN")
                    )
            );

    private final AntPathMatcher pathMatcher =
            new AntPathMatcher();

    /**
     * 현재 요청 경로에 설정된 역할 규칙을 조회합니다.
     *
     * 역할 제한이 없는 경로라면 Optional.empty()를 반환합니다.
     */
    public Optional<Set<String>> findAllowedRoles(
            ServerWebExchange exchange
    ) {
        String requestPath =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        return ROLE_RULES.stream()
                .filter(rule ->
                        rule.matches(
                                requestPath,
                                pathMatcher
                        )
                )
                .map(RoleRule::allowedRoles)
                .findFirst();
    }

    /**
     * 역할 제한이 적용되는 요청 경로와
     * 접근 가능한 역할을 관리합니다.
     */
    private record RoleRule(
            String pathPattern,
            Set<String> allowedRoles
    ) {

        private boolean matches(
                String requestPath,
                AntPathMatcher pathMatcher
        ) {
            return pathMatcher.match(
                    pathPattern,
                    requestPath
            );
        }
    }
}