package com.bubbletea.order.application;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.event.BillingRequestedEvent;
import com.bubbletea.order.infrastructure.kafka.OrderKafkaTopic;
import com.bubbletea.order.infrastructure.outbox.OutboxRecorder;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingRequestPublisher {

  private static final String AGGREGATE_TYPE = "Subscription";
  private static final String EVENT_TYPE = "BillingRequested";
  private static final String CURRENCY = "KRW";
  private static final String ORDER_NAME_FALLBACK = "구독 결제"; // 구독에 상품명이 없을 때의 폴백
  private static final String USER_HEADER = "X-User-Id";
  /**
   * Toss 주문번호 포맷. Toss 는 orderId 를 <b>6자 이상 64자 이하</b>로 제한하므로(영문·숫자·{@code -}·{@code _}),
   * 주문 ID 를 6자리로 0-패딩해 한 자리 주문 ID(예: {@code SUB-4} = 5자)가 {@code INVALID_ORDER_ID} 로
   * 거절되는 것을 막는다. 패딩은 유일성에 영향을 주지 않는다.
   */
  private static final String TOSS_ORDER_ID_FORMAT = "SUB-%06d";

  private final OutboxRecorder outboxRecorder;

  public void publish(SubscriptionOrder order) {
    BillingSchedule schedule = order.getBillingSchedule();
    Subscription subscription = schedule.getSubscription();

    String tossOrderId = String.format(TOSS_ORDER_ID_FORMAT, order.getId());
    String orderName =
        (subscription.getProductName() != null) ? subscription.getProductName() : ORDER_NAME_FALLBACK;
    BillingRequestedEvent event = BillingRequestedEvent.of(
        order.getId(), schedule.getPaymentMethodId(), tossOrderId,
        CURRENCY, orderName, order.getAmount());

    // key=orderId, header X-User-Id=memberId (payment 소비자 계약)
    outboxRecorder.record(AGGREGATE_TYPE, subscription.getId(), EVENT_TYPE,
        OrderKafkaTopic.PAYMENT_REQUESTED, String.valueOf(order.getId()),
        Map.of(USER_HEADER, String.valueOf(subscription.getMemberId())), event);
  }
}
