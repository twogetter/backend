package com.bubbletea.order.domain.event;

import java.math.BigDecimal;

/**
 * 정기결제 배치가 payment-service로 결제를 요청할 때 발행하는 이벤트.
 * <p>
 * ⚠️ payment-service의 {@code com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent}와
 * <b>필드명·타입이 정확히 일치</b>해야 한다(JSON 역직렬화 계약). 토픽/메시지키/헤더 규약:
 * <ul>
 *   <li>topic: {@code payment.order.payment-requested}</li>
 *   <li>key: orderId</li>
 *   <li>header: {@code X-User-Id} = memberId</li>
 * </ul>
 * 후속 확인 필요 필드: {@code tossOrderId}(생성 규약), {@code orderName}(표시명 출처), {@code currency}.
 */
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
    return new BillingRequestedEvent(orderId, methodId, tossOrderId, totalAmount.longValue(),
        currency, orderName, totalAmount);
  }
}
