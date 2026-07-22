package com.bubbletea.payment.service.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Builder
public record PaymentCancelRequestDto(
        Long paymentId,
        BigDecimal cancelAmount,
        String cancelReason,
        String idempotencyKey
) {
}
