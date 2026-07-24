package com.bubbletea.order.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.order.domain.enums.AttemptStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaymentAttempt 엔티티")
class PaymentAttemptTest {

  private SubscriptionOrder newOrder() {
    Subscription subscription = new Subscription(1L, 10L, "구독권");
    BillingSchedule schedule = new BillingSchedule(subscription, 5L, BigDecimal.valueOf(9900));
    return schedule.createOrder();
  }

  @Test
  @DisplayName("success() 는 SUCCESS 상태이고 실패 사유가 없다")
  void success() {
    SubscriptionOrder order = newOrder();

    PaymentAttempt attempt = PaymentAttempt.success(order, 1);

    assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUCCESS);
    assertThat(attempt.getSequence()).isEqualTo(1);
    assertThat(attempt.getFailReason()).isNull();
    assertThat(attempt.getSubscriptionOrder()).isSameAs(order);
    assertThat(attempt.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("fail() 은 FAIL 상태이고 실패 사유를 보관한다")
  void fail() {
    SubscriptionOrder order = newOrder();

    PaymentAttempt attempt = PaymentAttempt.fail(order, 2, "카드 한도 초과");

    assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FAIL);
    assertThat(attempt.getSequence()).isEqualTo(2);
    assertThat(attempt.getFailReason()).isEqualTo("카드 한도 초과");
  }
}
