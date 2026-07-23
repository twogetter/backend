package com.bubbletea.order.domain.event;

import java.time.LocalDateTime;

/**
 * 신규 구독 결제 성공 시 채팅방 팬 입장을 위해 chat-service 로 발행하는 이벤트.
 *
 * chat-service 소비자 계약(OrderCreatedEvent{fanId, artistId, startedAt})과
 * 필드명을 일치시켜야 역직렬화된다. artistId 는 상품의 pid(=artistId) 를 사용한다.
 */
public record SubscriptionActivatedEvent(
    Long fanId,
    Long artistId,
    LocalDateTime startedAt
) {
  public static SubscriptionActivatedEvent of(Long fanId, Long artistId, LocalDateTime startedAt) {
    return new SubscriptionActivatedEvent(fanId, artistId, startedAt);
  }
}
