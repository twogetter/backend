package com.bubbletea.payment.service.dto;

import java.math.BigDecimal;

public record PaymentConfirmRequestDto(
        String tossOrderId,
        Long paymentMethodId,
        BigDecimal amount
) {
}
