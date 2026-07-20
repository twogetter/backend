package com.bubbletea.gateway.authentication.support;

public record AuthenticationErrorResponse(
        String code,
        String message
) {
}
