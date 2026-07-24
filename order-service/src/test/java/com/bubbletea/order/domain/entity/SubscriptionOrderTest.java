package com.bubbletea.order.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.order.domain.enums.OrderStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubscriptionOrder 엔티티")
class SubscriptionOrderTest {

  private static final BigDecimal AMOUNT = BigDecimal.valueOf(4900);

  private SubscriptionOrder newOrder() {
    Subscription subscription = new Subscription(1L, 10L, "구독권");
    BillingSchedule schedule = new BillingSchedule(subscription, 5L, AMOUNT);
    return schedule.createOrder();
  }

  @Test
  @DisplayName("생성 시 PENDING 상태이고 retryCount 는 0 이다")
  void createsInPendingState() {
    SubscriptionOrder order = newOrder();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getRetryCount()).isZero();
    assertThat(order.getAmount()).isEqualByComparingTo(AMOUNT);
    assertThat(order.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("complete() 는 COMPLETED 로 전이한다")
  void complete() {
    SubscriptionOrder order = newOrder();

    order.complete();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
  }

  @Test
  @DisplayName("fail() 은 FAILED 로 전이하되 소프트 삭제는 하지 않는다")
  void fail() {
    SubscriptionOrder order = newOrder();

    order.fail();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
    assertThat(order.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("softDelete() 는 FAILED 로 전이하고 deletedAt 을 채운다")
  void softDelete() {
    SubscriptionOrder order = newOrder();

    order.softDelete();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
    assertThat(order.getDeletedAt()).isNotNull();
  }
}
