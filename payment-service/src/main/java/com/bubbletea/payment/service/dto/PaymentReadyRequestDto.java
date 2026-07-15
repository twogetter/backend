package com.bubbletea.payment.service.dto;

public record PaymentReadyRequestDto(
        Long orderId,
        String tossOrderId,
        String currency,
        Long amount,
        String customerKey,
        Long selectedMethodId
) {
}
