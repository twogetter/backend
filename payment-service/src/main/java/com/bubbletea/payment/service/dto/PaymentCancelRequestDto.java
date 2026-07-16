package com.bubbletea.payment.service.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCancelRequestDto(
        Long paymentId,
        BigDecimal cancelAmount,
        String cancelReason,
        String idempotencyKey
) {
}
