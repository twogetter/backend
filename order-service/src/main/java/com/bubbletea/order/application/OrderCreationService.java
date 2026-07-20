package com.bubbletea.order.application;

import com.bubbletea.order.domain.dto.CreatedOrderContextDto;
import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import com.bubbletea.order.domain.repository.SubscriptionRepository;
import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TX1: 결제 전 구독/정기결제 스케줄/주문을 PENDING 상태로 생성하고 커밋한다.
 * 외부 호출을 포함하지 않는 순수 DB 트랜잭션으로, 결제 호출과 트랜잭션을 분리한다.
 */
@Service
@RequiredArgsConstructor
public class OrderCreationService {

  private final SubscriptionRepository subscriptionRepository;
  private final BillingScheduleRepository billingScheduleRepository;
  private final SubscriptionOrderRepository subscriptionOrderRepository;

  @Transactional
  public CreatedOrderContextDto createPendingOrder(Long memberId, ProductInfoResponseDto product,
      Long paymentMethodId) {
    Subscription subscription = new Subscription(memberId, product.productId(), product.productName());
    BillingSchedule schedule =
        new BillingSchedule(subscription, paymentMethodId, product.price());
    SubscriptionOrder order = schedule.createOrder();

    subscriptionRepository.save(subscription);
    billingScheduleRepository.save(schedule);
    subscriptionOrderRepository.save(order);

    return new CreatedOrderContextDto(subscription.getId(), schedule.getId(), order.getId(),
        memberId, product.productId(), order.getAmount());
  }
}
