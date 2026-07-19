package com.bubbletea.order.application;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.PaymentAttempt;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.event.PaymentResultEvent;
import com.bubbletea.order.domain.repository.PaymentAttemptRepository;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingResultService {

  private final SubscriptionOrderRepository subscriptionOrderRepository;
  private final PaymentAttemptRepository paymentAttemptRepository;

  @Transactional
  public void handle(PaymentResultEvent event) {
    SubscriptionOrder order = subscriptionOrderRepository.findById(event.orderId()).orElse(null);
    if (order == null) {
      log.warn("[BillingResult] 주문 없음 — 스킵. orderId={}, status={}", event.orderId(), event.status());
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
    if (order.getStatus() != OrderStatus.PENDING) {
      log.info("[BillingResult] 이미 처리된 주문 — 스킵. orderId={}, status={}", order.getId(), order.getStatus());
      return;
    }
    BillingSchedule schedule = order.getBillingSchedule();
    order.complete();
    schedule.renew();
    paymentAttemptRepository.save(PaymentAttempt.success(order, nextSequence(order)));
    log.info("[BillingResult] 정기결제 성공. orderId={}, nextBillingDate={}",
        order.getId(), schedule.getNextBillingDate());
  }

  private void onFailure(SubscriptionOrder order, String reason) {
    if (order.getStatus() != OrderStatus.PENDING) {
      log.info("[BillingResult] 이미 처리된 주문 — 스킵. orderId={}, status={}", order.getId(), order.getStatus());
      return;
    }
    Subscription subscription = order.getBillingSchedule().getSubscription();
    order.fail();
    paymentAttemptRepository.save(PaymentAttempt.fail(order, nextSequence(order), reason));
    subscription.pause();
    log.warn("[BillingResult] 정기결제 실패 → 구독 일시정지. orderId={}, reason={}", order.getId(), reason);
  }

  private int nextSequence(SubscriptionOrder order) {
    return paymentAttemptRepository.countBySubscriptionOrder(order) + 1;
  }
}
