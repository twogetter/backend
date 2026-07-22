package com.bubbletea.payment.infrastructure.kafka.dto;

import java.math.BigDecimal;

public record BillingEvent(
        Long orderId,
        Long methodId,
        String tossOrderId,
        Long amount,
        String currency,
        String orderName,
        BigDecimal totalAmount
) {


}
