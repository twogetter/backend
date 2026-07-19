package com.bubbletea.order.domain.repository;

import com.bubbletea.order.domain.entity.SubscriptionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionOrderRepository extends JpaRepository<SubscriptionOrder, Long> {

}
