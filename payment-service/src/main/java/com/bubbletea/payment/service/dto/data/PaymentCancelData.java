package com.bubbletea.payment.service.dto.data;

import com.bubbletea.payment.entity.enums.PaymentStatus;
import java.math.BigDecimal;

public record PaymentCancelData(
        Long paymentCancelId,
        Long paymentId,
        String paymentKey,
        String idempotencyKey,
        BigDecimal cancelAmount,
        String cancelReason
) {
}
