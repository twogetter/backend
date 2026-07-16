package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.PaymentCancel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {
    Optional<PaymentCancel> findByIdempotencyKey(String idempotencyKey);
}
