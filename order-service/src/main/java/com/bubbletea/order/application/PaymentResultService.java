package com.bubbletea.order.application;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.order.domain.dto.CreatedOrderContextDto;
import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.PaymentAttempt;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.event.PaymentFailedEvent;
import com.bubbletea.order.domain.event.SubscriptionActivatedEvent;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import com.bubbletea.order.domain.repository.PaymentAttemptRepository;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import com.bubbletea.order.infrastructure.kafka.OrderKafkaTopic;
import com.bubbletea.order.infrastructure.outbox.OutboxRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TX2: 결제 결과를 반영한다.
 *   성공 → 주문 완료 + 구독/스케줄 활성화 + {@code SubscriptionActivated} 아웃박스 기록(채팅방 생성 이벤트).
 *   실패 → {@code PaymentFailed}(사유) 아웃박스 기록 후 구독/스케줄/주문 롤백(삭제). 재시도 없음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentResultService {

  private static final int FIRST_ATTEMPT = 1;
  private static final String AGGREGATE_TYPE = "Subscription";

  private final SubscriptionOrderRepository subscriptionOrderRepository;
  private final PaymentAttemptRepository paymentAttemptRepository;
  private final OutboxRecorder outboxRecorder;

  @Transactional
  public OrderStatus handlePaymentResult(CreatedOrderContextDto ctx, boolean success, String failReason) {
    SubscriptionOrder order = subscriptionOrderRepository.findById(ctx.orderId())
        .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));

    // 멱등 가드: PENDING이 아니면 이미 처리된 주문이므로 중복 처리(PaymentAttempt 중복,
    // 아웃박스 중복 발행 → 채팅방 중복 생성, nextBillingDate 재연장)를 막고 현재 상태를 그대로 반환한다.
    if (order.getStatus() != OrderStatus.PENDING) {
      log.info("[Subscription] 이미 처리된 주문 — 중복 처리 스킵. orderId={}, status={}",
          ctx.orderId(), order.getStatus());
      return order.getStatus();
    }

    return success ? activate(order, ctx) : rollback(order, ctx, failReason);
  }

  private OrderStatus activate(SubscriptionOrder order, CreatedOrderContextDto ctx) {
    BillingSchedule schedule = order.getBillingSchedule();
    Subscription subscription = schedule.getSubscription();

    order.complete();
    schedule.activate();
    subscription.activate();
    paymentAttemptRepository.save(PaymentAttempt.success(order, FIRST_ATTEMPT));

    // 채팅방 생성을 위한 이벤트 (도메인 커밋과 동일 트랜잭션에서 아웃박스 기록)
    outboxRecorder.record(AGGREGATE_TYPE, ctx.subscriptionId(), "SubscriptionActivated",
        OrderKafkaTopic.SUBSCRIPTION_ACTIVATED, String.valueOf(ctx.memberId()),
        SubscriptionActivatedEvent.of(ctx.memberId(), ctx.productId(), ctx.subscriptionId()));

    log.info("[Subscription] 신규 구독 활성화 완료. subscriptionId={}, orderId={}",
        ctx.subscriptionId(), ctx.orderId());
    return OrderStatus.COMPLETED;
  }

  private OrderStatus rollback(SubscriptionOrder order, CreatedOrderContextDto ctx, String failReason) {
    BillingSchedule schedule = order.getBillingSchedule();
    Subscription subscription = schedule.getSubscription();

    // 1) 결제 실패 시도 이력 기록
    paymentAttemptRepository.save(PaymentAttempt.fail(order, FIRST_ATTEMPT, failReason));

    // 2) 실패 사유 아웃박스에 기록
    outboxRecorder.record(AGGREGATE_TYPE, ctx.subscriptionId(), "PaymentFailed",
        OrderKafkaTopic.PAYMENT_FAILED, String.valueOf(ctx.memberId()),
        PaymentFailedEvent.of(ctx.memberId(), ctx.productId(), ctx.orderId(), failReason));

    // 3) 주문/스케줄/구독 소프트 삭제
    order.softDelete();
    schedule.softDelete();
    subscription.softDelete();

    log.warn("[Subscription] 결제 실패로 구독 소프트 삭제. subscriptionId={}, orderId={}, reason={}",
        ctx.subscriptionId(), ctx.orderId(), failReason);
    return OrderStatus.FAILED;
  }
}
