package com.bubbletea.order.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.order.domain.enums.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Subscription 엔티티")
class SubscriptionTest {

  @Test
  @DisplayName("생성 시 PENDING 상태이며 필드가 초기화된다")
  void createsInPendingState() {
    Subscription subscription = new Subscription(1L, 10L, "구독권");

    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
    assertThat(subscription.getMemberId()).isEqualTo(1L);
    assertThat(subscription.getProductId()).isEqualTo(10L);
    assertThat(subscription.getProductName()).isEqualTo("구독권");
    assertThat(subscription.getEndedAt()).isNull();
    assertThat(subscription.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("activate() 는 상태를 ACTIVE 로 전이한다")
  void activate() {
    Subscription subscription = new Subscription(1L, 10L, "구독권");

    subscription.activate();

    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  @DisplayName("pause() 는 상태를 PAUSED 로 전이한다")
  void pause() {
    Subscription subscription = new Subscription(1L, 10L, "구독권");

    subscription.pause();

    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
  }

  @Test
  @DisplayName("softDelete() 는 CANCELED 로 전이하고 endedAt·deletedAt 을 채운다")
  void softDelete() {
    Subscription subscription = new Subscription(1L, 10L, "구독권");

    subscription.softDelete();

    assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    assertThat(subscription.getEndedAt()).isNotNull();
    assertThat(subscription.getDeletedAt()).isNotNull();
  }
}
