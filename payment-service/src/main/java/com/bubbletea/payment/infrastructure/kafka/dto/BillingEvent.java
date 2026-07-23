package com.bubbletea.payment.infrastructure.kafka.dto;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record BillingEvent(
        Long orderId,
        Long methodId,
        String tossOrderId,
        String currency,
        String orderName,
        BigDecimal totalAmount
) {


}
