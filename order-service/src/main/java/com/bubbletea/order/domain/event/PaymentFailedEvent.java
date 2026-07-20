package com.bubbletea.order.domain.event;

/** 신규 구독 결제 실패 시 아웃박스에 기록·발행되는 이벤트(실패 사유의 durable 기록 겸용). */
public record PaymentFailedEvent(
    Long memberId,
    Long productId,
    Long orderId,
    String failReason
) {
  public static PaymentFailedEvent of(Long memberId, Long productId, Long orderId, String failReason) {
    return new PaymentFailedEvent(memberId, productId, orderId, failReason);
  }
}
