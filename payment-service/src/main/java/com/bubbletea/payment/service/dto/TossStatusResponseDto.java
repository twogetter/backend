package com.bubbletea.payment.service.dto;

import java.util.List;

public record TossStatusResponseDto(
        String paymentKey,
        String orderId,
        String status,
        Long totalAmount,
        String method,
        List<TossCancel> cancels
) {
    public boolean isCancelCompleted() {
        return "CANCELED".equals(this.status) || (cancels != null && !cancels.isEmpty());
    }

    public record TossCancel(
            Long cancelAmount,
            String cancelReason,
            String canceledAt,
            String idempotencyKey,
            String transactionKey,
            String receiptKey
    ) {}
}
