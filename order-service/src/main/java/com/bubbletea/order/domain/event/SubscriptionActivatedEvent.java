package com.bubbletea.order.domain.event;

/** 신규 구독 결제 성공 시 채팅방 생성을 위해 chat-service 로 발행하는 이벤트. */
public record SubscriptionActivatedEvent(
    Long memberId,
    Long productId,
    Long subscriptionId
) {
  public static SubscriptionActivatedEvent of(Long memberId, Long productId, Long subscriptionId) {
    return new SubscriptionActivatedEvent(memberId, productId, subscriptionId);
  }
}
