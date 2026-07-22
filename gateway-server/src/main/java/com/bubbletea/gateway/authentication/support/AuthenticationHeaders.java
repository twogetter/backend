package com.bubbletea.gateway.authentication.support;

public final class AuthenticationHeaders {

    public static final String AUTHORIZATION =
            "Authorization";

    public static final String BEARER_PREFIX =
            "Bearer ";

    public static final String USER_ID =
            "X-User-Id";

    public static final String USER_ROLE =
            "X-User-Role";

    /**
     * Base64 URL 인코딩된 UTF-8 닉네임
     */
    public static final String USER_NICKNAME =
            "X-User-Nickname";

    private AuthenticationHeaders() {
    }
}