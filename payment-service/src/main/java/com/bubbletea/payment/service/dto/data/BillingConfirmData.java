package com.bubbletea.payment.service.dto.data;

import lombok.Builder;

@Builder
public record BillingConfirmData(
        Long paymentId,
        String customerKey,
        String methodKey,
        String idempotencyKey
) {
}
