package com.bubbletea.order.application;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import com.bubbletea.order.domain.event.BillingRequestedEvent;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import com.bubbletea.order.infrastructure.kafka.OrderKafkaTopic;
import com.bubbletea.order.infrastructure.outbox.OutboxRecorder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringBillingService {

  private static final String AGGREGATE_TYPE = "Subscription";
  private static final String EVENT_TYPE = "BillingRequested";
  private static final String CURRENCY = "KRW";
  private static final String ORDER_NAME = "정기 구독 결제"; // 구독에 상품명이 없을 때의 폴백
  private static final String USER_HEADER = "X-User-Id";
  private static final String TOSS_ORDER_ID_PREFIX = "SUB-";

  private final BillingScheduleRepository billingScheduleRepository;
  private final SubscriptionOrderRepository subscriptionOrderRepository;
  private final OutboxRecorder outboxRecorder;

  /** 오늘 기준 결제 예정일이 도래한 활성 스케줄 ID 목록. */
  @Transactional(readOnly = true)
  public List<Long> findDueScheduleIds() {
    return billingScheduleRepository
        .findByStatusAndNextBillingDateLessThanEqual(SubscriptionStatus.ACTIVE, LocalDate.now())
        .stream()
        .map(BillingSchedule::getId)
        .toList();
  }

  /**
   * 스케줄 1건에 대해 회차 주문 생성 + 결제요청 아웃박스 기록을 한 트랜잭션으로 처리한다.
   * 스케줄별로 트랜잭션을 분리해 한 건 실패가 다른 건에 영향을 주지 않게 한다.
   */
  @Transactional
  public void processDueSchedule(Long scheduleId) {
    BillingSchedule schedule = billingScheduleRepository.findById(scheduleId).orElse(null);
    if (schedule == null || schedule.getStatus() != SubscriptionStatus.ACTIVE) {
      return;
    }
    // 멱등 가드: 아직 결과 미확정(PENDING) 주문이 있으면 중복 청구하지 않는다.
    if (subscriptionOrderRepository.existsByBillingScheduleAndStatus(schedule, OrderStatus.PENDING)) {
      log.info("[Billing] 진행 중 주문 존재 — 스킵. scheduleId={}", scheduleId);
      return;
    }

    Subscription subscription = schedule.getSubscription();
    SubscriptionOrder order = schedule.createOrder();
    subscriptionOrderRepository.save(order);

    String tossOrderId = TOSS_ORDER_ID_PREFIX + order.getId();
    String orderName = (subscription.getProductName() != null) ? subscription.getProductName() : ORDER_NAME;
    BillingRequestedEvent event = BillingRequestedEvent.of(
        order.getId(), schedule.getPaymentMethodId(), tossOrderId,
        CURRENCY, orderName, order.getAmount());

    // key=orderId, header X-User-Id=memberId (payment 소비자 계약)
    outboxRecorder.record(AGGREGATE_TYPE, subscription.getId(), EVENT_TYPE,
        OrderKafkaTopic.PAYMENT_REQUESTED, String.valueOf(order.getId()),
        Map.of(USER_HEADER, String.valueOf(subscription.getMemberId())), event);

    log.info("[Billing] 정기결제 요청 발행. scheduleId={}, orderId={}, amount={}",
        scheduleId, order.getId(), order.getAmount());
  }
}
