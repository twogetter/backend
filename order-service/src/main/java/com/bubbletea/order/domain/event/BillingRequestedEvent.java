package com.bubbletea.order.domain.event;

import java.math.BigDecimal;

public record BillingRequestedEvent(
    Long orderId,
    Long methodId,
    String tossOrderId,
    Long amount,
    String currency,
    String orderName,
    BigDecimal totalAmount
) {

  public static BillingRequestedEvent of(Long orderId, Long methodId, String tossOrderId,
      String currency, String orderName, BigDecimal totalAmount) {
    // 소수부가 있으면 예외 발생(주문/결제 금액 불일치 방지).
    // longValueExact() 소수부가 있으면 ArithmeticException → 배치가 해당 스케줄을 건너뛰고(롤백) 로그 기록
    return new BillingRequestedEvent(orderId, methodId, tossOrderId, totalAmount.longValueExact(),
        currency, orderName, totalAmount);
  }
}
