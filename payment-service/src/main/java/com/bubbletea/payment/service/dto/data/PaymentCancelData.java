package com.bubbletea.payment.service.dto.data;

import com.bubbletea.payment.entity.enums.PaymentStatus;

public record PaymentCancelData(
        Long paymentId,
        String paymentKey,
        String idempotencyKey,
        Long cancelAmount,
        String cancelReason
) {
}
