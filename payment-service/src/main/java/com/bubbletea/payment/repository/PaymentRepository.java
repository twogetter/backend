package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTossOrderId(String tossOrderId);

    List<Payment> findTop100ByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime createdAt);

    Optional<Payment> findByOrderId(Long orderId);
}
