package com.bubbletea.order.domain.repository;

import com.bubbletea.order.domain.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

}
