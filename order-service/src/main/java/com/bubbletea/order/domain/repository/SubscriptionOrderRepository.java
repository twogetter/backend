package com.bubbletea.order.domain.repository;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionOrderRepository extends JpaRepository<SubscriptionOrder, Long> {

  /** 해당 스케줄에 아직 결과 미확정(PENDING) 주문이 있으면 배치가 중복 청구하지 않도록 가드. */
  boolean existsByBillingScheduleAndStatus(BillingSchedule billingSchedule, OrderStatus status);
}
