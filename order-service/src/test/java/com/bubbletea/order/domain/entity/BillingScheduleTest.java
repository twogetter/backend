package com.bubbletea.order.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("BillingSchedule 엔티티")
class BillingScheduleTest {

  private static final BigDecimal AMOUNT = BigDecimal.valueOf(9900);

  private BillingSchedule newSchedule() {
    Subscription subscription = new Subscription(1L, 10L, "구독권");
    return new BillingSchedule(subscription, 5L, AMOUNT);
  }

  @Test
  @DisplayName("생성 시 PENDING 상태다")
  void createsInPendingState() {
    BillingSchedule schedule = newSchedule();

    assertThat(schedule.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
    assertThat(schedule.getAmount()).isEqualByComparingTo(AMOUNT);
    assertThat(schedule.getPaymentMethodId()).isEqualTo(5L);
  }

  @Test
  @DisplayName("createOrder() 는 동일 금액의 PENDING 주문을 생성한다")
  void createOrder() {
    BillingSchedule schedule = newSchedule();

    SubscriptionOrder order = schedule.createOrder();

    assertThat(order.getAmount()).isEqualByComparingTo(AMOUNT);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getBillingSchedule()).isSameAs(schedule);
  }

  @Test
  @DisplayName("activate() 는 ACTIVE 로 전이하고 다음 결제일을 한 달 뒤로 설정한다")
  void activate() {
    BillingSchedule schedule = newSchedule();

    schedule.activate();

    assertThat(schedule.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(schedule.getNextBillingDate()).isEqualTo(LocalDate.now().plusMonths(1));
  }

  @Test
  @DisplayName("renew() 는 nextBillingDate 가 없으면 한 달 뒤로 설정한다")
  void renewFromNull() {
    BillingSchedule schedule = newSchedule();

    schedule.renew();

    assertThat(schedule.getNextBillingDate()).isEqualTo(LocalDate.now().plusMonths(1));
  }

  @Test
  @DisplayName("renew() 는 다음 결제일이 과거면 미래가 될 때까지 이월한다")
  void renewSkipsPastDates() {
    BillingSchedule schedule = newSchedule();
    // 배치 지연 등으로 다음 결제일이 과거(3개월 전)인 상황을 재현
    ReflectionTestUtils.setField(schedule, "nextBillingDate", LocalDate.now().minusMonths(3));

    schedule.renew();

    assertThat(schedule.getNextBillingDate()).isAfter(LocalDate.now());
  }

  @Test
  @DisplayName("softDelete() 는 CANCELED 로 전이하고 deletedAt 을 채운다")
  void softDelete() {
    BillingSchedule schedule = newSchedule();

    schedule.softDelete();

    assertThat(schedule.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    assertThat(schedule.getDeletedAt()).isNotNull();
  }
}
