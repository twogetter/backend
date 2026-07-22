package com.bubbletea.payment.infrastructure.kafka.dto;

import java.math.BigDecimal;

public record PaymentResultEvent(
        Long orderId,
        Long paymentId,
        BigDecimal amount,
        String paymentKey,
        String status,
        String reason
) {
}
