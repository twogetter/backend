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

@Service
@RequiredArgsConstructor
public class OrderCreationService {

  private final SubscriptionRepository subscriptionRepository;
  private final BillingScheduleRepository billingScheduleRepository;
  private final SubscriptionOrderRepository subscriptionOrderRepository;
  private final BillingRequestPublisher billingRequestPublisher;

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

    // 주문 커밋과 동일 트랜잭션에서 결제요청을 아웃박스에 기록(dual-write 방지)
    billingRequestPublisher.publish(order);

    return new CreatedOrderContextDto(subscription.getId(), schedule.getId(), order.getId(),
        memberId, product.productId(), order.getAmount());
  }
}
