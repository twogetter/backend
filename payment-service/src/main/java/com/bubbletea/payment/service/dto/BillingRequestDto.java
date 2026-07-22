package com.bubbletea.payment.service.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record BillingRequestDto(
        String customerKey,
        String methodKey,
        String orderId,
        String orderName,
        Long amount
) {
}
