package com.bubbletea.payment.service.dto.data;

import java.math.BigDecimal;

public record PaymentConfirmData(
        Long paymentId,
        String idempotencyKey,
        BigDecimal amount
) {
}
