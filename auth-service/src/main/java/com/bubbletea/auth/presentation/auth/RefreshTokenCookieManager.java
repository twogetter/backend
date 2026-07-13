package com.bubbletea.auth.presentation.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieManager {

    public static final String COOKIE_NAME = "refreshToken";

    private static final String COOKIE_PATH = "/api/auth";

    private final boolean secure;
    private final String sameSite;

    public RefreshTokenCookieManager(
            @Value("${auth.cookie.secure:false}")
            boolean secure,

            @Value("${auth.cookie.same-site:Lax}")
            String sameSite
    ) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public ResponseCookie create(
            String refreshToken,
            long expirationSeconds
    ) {
        return ResponseCookie.from(
                        COOKIE_NAME,
                        refreshToken
                )
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(expirationSeconds))
                .build();
    }

    public ResponseCookie delete() {
        return ResponseCookie.from(
                        COOKIE_NAME,
                        ""
                )
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}