package com.bubbletea.order.domain.repository;

import com.bubbletea.order.domain.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

}
