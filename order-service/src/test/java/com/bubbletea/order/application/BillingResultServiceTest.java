package com.bubbletea.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.PaymentAttempt;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.enums.AttemptStatus;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import com.bubbletea.order.domain.event.PaymentResultEvent;
import com.bubbletea.order.domain.event.SubscriptionActivatedEvent;
import com.bubbletea.order.domain.repository.PaymentAttemptRepository;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import com.bubbletea.order.infrastructure.kafka.OrderKafkaTopic;
import com.bubbletea.order.infrastructure.outbox.OutboxRecorder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingResultService — 결제 결과 반영(TX2)")
class BillingResultServiceTest {

  private static final Long ORDER_ID = 100L;
  private static final Long MEMBER_ID = 1L;
  private static final Long PRODUCT_ID = 10L;
  private static final BigDecimal AMOUNT = BigDecimal.valueOf(9900);

  @Mock
  private SubscriptionOrderRepository subscriptionOrderRepository;
  @Mock
  private PaymentAttemptRepository paymentAttemptRepository;
  @Mock
  private OutboxRecorder outboxRecorder;

  @InjectMocks
  private BillingResultService billingResultService;

  /** subscription → schedule → order 애그리거트를 PENDING 상태로 구성. */
  private SubscriptionOrder pendingOrder() {
    Subscription subscription = new Subscription(MEMBER_ID, PRODUCT_ID, "구독권");
    BillingSchedule schedule = new BillingSchedule(subscription, 5L, AMOUNT);
    return schedule.createOrder();
  }

  private PaymentResultEvent event(String status, String reason) {
    return new PaymentResultEvent(ORDER_ID, 999L, AMOUNT, "pay_key", status, reason);
  }

  @Nested
  @DisplayName("가드")
  class Guard {

    @Test
    @DisplayName("주문이 없으면 아무것도 하지 않는다")
    void skipsWhenOrderNotFound() {
      when(subscriptionOrderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

      billingResultService.handle(event(PaymentResultEvent.STATUS_SUCCEEDED, null));

      verifyNoInteractions(paymentAttemptRepository, outboxRecorder);
    }

    @Test
    @DisplayName("이미 처리된(PENDING 이 아닌) 주문은 스킵한다")
    void skipsWhenOrderNotPending() {
      SubscriptionOrder order = pendingOrder();
      order.complete();
      when(subscriptionOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

      billingResultService.handle(event(PaymentResultEvent.STATUS_SUCCEEDED, null));

      verifyNoInteractions(paymentAttemptRepository, outboxRecorder);
    }

    @Test
    @DisplayName("UNKNOWN 결과는 상태를 바꾸지 않는다")
    void unknownIsNoOp() {
      SubscriptionOrder order = pendingOrder();
      when(subscriptionOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

      billingResultService.handle(event(PaymentResultEvent.STATUS_UNKNOWN, null));

      assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
      verifyNoInteractions(paymentAttemptRepository, outboxRecorder);
    }

    @Test
    @DisplayName("미지원 상태값은 상태를 바꾸지 않는다")
    void unsupportedStatusIsNoOp() {
      SubscriptionOrder order = pendingOrder();
      when(subscriptionOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

      billingResultService.handle(event("SomethingElse", null));

      assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
      verifyNoInteractions(paymentAttemptRepository, outboxRecorder);
    }
  }

  @Nested
  @DisplayName("결제 성공")
  class OnSuccess {

    @Test
    @DisplayName("신규 구독(첫 결제)은 구독·스케줄을 활성화하고 SubscriptionActivated 를 아웃박스에 기록한다")
    void firstPaymentActivatesAndRecordsOutbox() {
      SubscriptionOrder order = pendingOrder();
      Subscription subscription = order.getBillingSchedule().getSubscription();
      when(subscriptionOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
      when(paymentAttemptRepository.countBySubscriptionOrder(order)).thenReturn(0);

      billingResultService.handle(event(PaymentResultEvent.STATUS_SUCCEEDED, null));

      assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
      assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(order.getBillingSchedule().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(order.getBillingSchedule().getNextBillingDate())
          .isEqualTo(LocalDate.now().plusMonths(1));

      ArgumentCaptor<PaymentAttempt> attempt = ArgumentCaptor.forClass(PaymentAttempt.class);
      verify(paymentAttemptRepository).save(attempt.capture());
      assertThat(attempt.getValue().getStatus()).isEqualTo(AttemptStatus.SUCCESS);
      assertThat(attempt.getValue().getSequence()).isEqualTo(1);

      verify(outboxRecorder).record(
          eq("Subscription"), any(), eq("SubscriptionActivated"),
          eq(OrderKafkaTopic.SUBSCRIPTION_ACTIVATED), eq(String.valueOf(MEMBER_ID)),
          eq(SubscriptionActivatedEvent.of(MEMBER_ID, PRODUCT_ID, subscription.getStartedAt())));
    }

    @Test
    @DisplayName("정기결제(갱신)는 다음 결제일만 이월하고 아웃박스에 기록하지 않는다")
    void renewalAdvancesNextBillingWithoutOutbox() {
      SubscriptionOrder order = pendingOrder();
      Subscription subscription = order.getBillingSchedule().getSubscription();
      subscription.activate(); // 이미 활성 구독 → 갱신 경로
      when(subscriptionOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
      when(paymentAttemptRepository.countBySubscriptionOrder(order)).thenReturn(1);

      billingResultService.handle(event(PaymentResultEvent.STATUS_SUCCEEDED, null));

      assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
      assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(order.getBillingSchedule().getNextBillingDate()).isAfter(LocalDate.now());
      verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
      verifyNoInteractions(outboxRecorder);
    }
  }

  @Nested
  @DisplayName("결제 실패")
  class OnFailure {

    @Test
    @DisplayName("신규 구독 실패는 주문·스케줄·구독을 소프트 삭제하고 PaymentFailed 를 기록한다")
    void firstPaymentFailureSoftDeletesAllAndRecordsOutbox() {
      SubscriptionOrder order = pendingOrder();
      Subscription subscription = order.getBillingSchedule().getSubscription();
      BillingSchedule schedule = order.getBillingSchedule();
      when(subscriptionOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
      when(paymentAttemptRepository.countBySubscriptionOrder(order)).thenReturn(0);

      billingResultService.handle(event(PaymentResultEvent.STATUS_FAILED, "카드 한도 초과"));

      assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
      assertThat(order.getDeletedAt()).isNotNull();
      assertThat(schedule.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
      assertThat(schedule.getDeletedAt()).isNotNull();
      assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
      assertThat(subscription.getDeletedAt()).isNotNull();

      ArgumentCaptor<PaymentAttempt> attempt = ArgumentCaptor.forClass(PaymentAttempt.class);
      verify(paymentAttemptRepository).save(attempt.capture());
      assertThat(attempt.getValue().getStatus()).isEqualTo(AttemptStatus.FAIL);
      assertThat(attempt.getValue().getFailReason()).isEqualTo("카드 한도 초과");

      verify(outboxRecorder).record(
          eq("Subscription"), any(), eq("PaymentFailed"),
          eq(OrderKafkaTopic.PAYMENT_FAILED), eq(String.valueOf(MEMBER_ID)), any());
    }

    @Test
    @DisplayName("정기결제 실패는 주문을 실패 처리하고 구독을 일시정지한다(소프트 삭제 없음)")
    void renewalFailurePausesSubscription() {
      SubscriptionOrder order = pendingOrder();
      Subscription subscription = order.getBillingSchedule().getSubscription();
      subscription.activate(); // 활성 구독의 갱신 실패
      when(subscriptionOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
      when(paymentAttemptRepository.countBySubscriptionOrder(order)).thenReturn(1);

      billingResultService.handle(event(PaymentResultEvent.STATUS_FAILED, "잔액 부족"));

      assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
      assertThat(order.getDeletedAt()).isNull();
      assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
      assertThat(subscription.getDeletedAt()).isNull();
      verify(paymentAttemptRepository).save(any(PaymentAttempt.class));
      verify(outboxRecorder, never()).record(any(), any(), any(), any(), any(), any());
    }
  }
}
