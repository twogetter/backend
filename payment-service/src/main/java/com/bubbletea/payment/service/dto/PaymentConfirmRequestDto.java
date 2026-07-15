package com.bubbletea.payment.service.dto;

import java.math.BigDecimal;

public record PaymentConfirmRequestDto(
        String paymentKey,
        String orderId,
        Long amount,
        String customerKey
        ) {
}
