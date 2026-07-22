package com.bubbletea.order.application;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.PaymentAttempt;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import com.bubbletea.order.domain.event.PaymentFailedEvent;
import com.bubbletea.order.domain.event.PaymentResultEvent;
import com.bubbletea.order.domain.event.SubscriptionActivatedEvent;
import com.bubbletea.order.domain.repository.PaymentAttemptRepository;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import com.bubbletea.order.infrastructure.kafka.OrderKafkaTopic;
import com.bubbletea.order.infrastructure.outbox.OutboxRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingResultService {

  private static final String AGGREGATE_TYPE = "Subscription";

  private final SubscriptionOrderRepository subscriptionOrderRepository;
  private final PaymentAttemptRepository paymentAttemptRepository;
  private final OutboxRecorder outboxRecorder;

  @Transactional
  public void handle(PaymentResultEvent event) {
    SubscriptionOrder order = subscriptionOrderRepository.findById(event.orderId()).orElse(null);
    if (order == null) {
      log.warn("[BillingResult] 주문 없음 — 스킵. orderId={}, status={}", event.orderId(), event.status());
      return;
    }
    if (order.getStatus() != OrderStatus.PENDING) {
      log.info("[BillingResult] 이미 처리된 주문 — 스킵. orderId={}, status={}", order.getId(), order.getStatus());
      return;
    }

    switch (event.status()) {
      case PaymentResultEvent.STATUS_SUCCEEDED -> onSuccess(order);
      case PaymentResultEvent.STATUS_FAILED -> onFailure(order, event.reason());
      case PaymentResultEvent.STATUS_UNKNOWN -> log.warn(
          "[BillingResult] 결제 보류(UNKNOWN) — 결과 대기. orderId={}", order.getId());
      default -> log.warn("[BillingResult] 미지원 status={} orderId={}", event.status(), order.getId());
    }
  }

  private void onSuccess(SubscriptionOrder order) {
    BillingSchedule schedule = order.getBillingSchedule();
    Subscription subscription = schedule.getSubscription();

    order.complete();
    paymentAttemptRepository.save(PaymentAttempt.success(order, nextSequence(order)));

    if (isFirstPayment(subscription)) {
      subscription.activate();
      schedule.activate();
      outboxRecorder.record(AGGREGATE_TYPE, subscription.getId(), "SubscriptionActivated",
          OrderKafkaTopic.SUBSCRIPTION_ACTIVATED, String.valueOf(subscription.getMemberId()),
          SubscriptionActivatedEvent.of(
              subscription.getMemberId(), subscription.getProductId(), subscription.getId()));
      log.info("[BillingResult] 신규 구독 활성화. subscriptionId={}, orderId={}",
          subscription.getId(), order.getId());
    } else {
      schedule.renew();
      log.info("[BillingResult] 정기결제 성공. orderId={}, nextBillingDate={}",
          order.getId(), schedule.getNextBillingDate());
    }
  }

  private void onFailure(SubscriptionOrder order, String reason) {
    BillingSchedule schedule = order.getBillingSchedule();
    Subscription subscription = schedule.getSubscription();

    paymentAttemptRepository.save(PaymentAttempt.fail(order, nextSequence(order), reason));

    if (isFirstPayment(subscription)) {
      // 신규 구독 실패 → 전체 취소(소프트 삭제) + 실패 알림
      outboxRecorder.record(AGGREGATE_TYPE, subscription.getId(), "PaymentFailed",
          OrderKafkaTopic.PAYMENT_FAILED, String.valueOf(subscription.getMemberId()),
          PaymentFailedEvent.of(
              subscription.getMemberId(), subscription.getProductId(), order.getId(), reason));
      order.softDelete();
      schedule.softDelete();
      subscription.softDelete();
      log.warn("[BillingResult] 신규 구독 결제 실패 → 취소. subscriptionId={}, orderId={}, reason={}",
          subscription.getId(), order.getId(), reason);
    } else {
      // 갱신 실패 → 구독 일시정지
      order.fail();
      subscription.pause();
      log.warn("[BillingResult] 정기결제 실패 → 구독 일시정지. orderId={}, reason={}", order.getId(), reason);
    }
  }

  /** 결과 시점에 구독이 아직 PENDING이면 첫 결제(신규 구독), ACTIVE면 갱신이다. */
  private boolean isFirstPayment(Subscription subscription) {
    return subscription.getStatus() == SubscriptionStatus.PENDING;
  }

  private int nextSequence(SubscriptionOrder order) {
    return paymentAttemptRepository.countBySubscriptionOrder(order) + 1;
  }
}
