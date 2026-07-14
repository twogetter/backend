package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTossOrderId(String tossOrderId);
}
