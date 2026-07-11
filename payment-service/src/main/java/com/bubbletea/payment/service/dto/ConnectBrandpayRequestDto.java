package com.bubbletea.payment.service.dto;

import lombok.Builder;

@Builder
public record ConnectBrandpayRequestDto(
        Long userId,
        String customerKey,
        String code
//        String accessToken
) {
}
