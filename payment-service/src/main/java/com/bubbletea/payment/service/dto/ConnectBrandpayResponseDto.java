package com.bubbletea.payment.service.dto;

import com.bubbletea.payment.entity.UserBrandpayAuth;
import lombok.Builder;

@Builder
public record ConnectBrandpayResponseDto(
        Long id,
        String customerKey,
        String accessToken
) {
    public static ConnectBrandpayResponseDto of(UserBrandpayAuth userBrandpayAuth) {
        return ConnectBrandpayResponseDto.builder()
                .id(userBrandpayAuth.getUserId())
                .customerKey(userBrandpayAuth.getCustomerKey())
                .accessToken(userBrandpayAuth.getAccessToken())
                .build();
    }
}
