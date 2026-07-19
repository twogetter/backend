package com.bubbletea.order.infrastructure.client.dto;

import java.math.BigDecimal;

/** payment-service 결제 요청 페이로드. */
public record PaymentRequestDto(
    Long orderId,
    Long memberId,
    BigDecimal amount,
    Long paymentMethodId
) {

}
