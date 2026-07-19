package com.bubbletea.order.domain.repository;

import com.bubbletea.order.domain.entity.PaymentAttempt;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

  /** 해당 주문의 결제 시도 횟수(다음 시도 sequence 산정용). */
  int countBySubscriptionOrder(SubscriptionOrder subscriptionOrder);
}
