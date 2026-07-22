package com.bubbletea.order.domain.event;

import java.math.BigDecimal;

public record PaymentResultEvent(
    Long orderId,
    Long paymentId,
    BigDecimal amount,
    String paymentKey,
    String status,
    String reason
) {

  public static final String STATUS_SUCCEEDED = "PaymentSucceeded";
  public static final String STATUS_FAILED = "PaymentFailed";
  public static final String STATUS_UNKNOWN = "PaymentUnknownEvent";
}
