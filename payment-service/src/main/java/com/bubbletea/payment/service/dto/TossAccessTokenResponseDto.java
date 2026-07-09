package com.bubbletea.payment.service.dto;

public record TossAccessTokenResponseDto(
    String accessToken,
    String refreshToken,
    String error,
    String message
) {}
