package com.bubbletea.order.infrastructure.client.dto;

/** payment-service 결제 처리 결과. */
public record PaymentResponseDto(
    boolean success,
    Long paymentId,
    String failReason
) {

}
