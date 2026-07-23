package com.bubbletea.order.application;

import com.bubbletea.order.domain.dto.CreatedOrderContextDto;
import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.entity.IdempotencyKey;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.bubbletea.order.domain.repository.IdempotencyKeyRepository;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import com.bubbletea.order.domain.repository.SubscriptionRepository;
import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreationService {

  private final SubscriptionRepository subscriptionRepository;
  private final BillingScheduleRepository billingScheduleRepository;
  private final SubscriptionOrderRepository subscriptionOrderRepository;
  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final BillingRequestPublisher billingRequestPublisher;

  @Transactional
  public CreatedOrderContextDto createPendingOrder(Long memberId, ProductInfoResponseDto product,
      Long paymentMethodId, String idempotencyKey) {
    Subscription subscription = new Subscription(memberId, product.productId(), product.productName());
    BillingSchedule schedule =
        new BillingSchedule(subscription, paymentMethodId, product.priceAmount());
    SubscriptionOrder order = schedule.createOrder();

    subscriptionRepository.save(subscription);
    billingScheduleRepository.save(schedule);
    subscriptionOrderRepository.save(order);

    // 멱등키 저장 — (member_id, idempotency_key) 유니크 제약이 동시 중복 요청을 원자적으로 차단
    // 위반 시 예외가 발생해 트랜잭션 전체 롤백
    idempotencyKeyRepository.save(
        new IdempotencyKey(memberId, idempotencyKey, order.getId(), subscription.getId()));

    // 주문 커밋과 동일 트랜잭션에서 결제요청을 아웃박스에 기록(dual-write 방지)
    billingRequestPublisher.publish(order);

    return new CreatedOrderContextDto(subscription.getId(), schedule.getId(), order.getId(),
        memberId, product.productId(), order.getAmount());
  }
}
