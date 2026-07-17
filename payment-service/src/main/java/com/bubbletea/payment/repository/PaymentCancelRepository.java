package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.enums.CancelStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {
    Optional<PaymentCancel> findByIdempotencyKey(String idempotencyKey);

    List<PaymentCancel> findByStatusAndCreatedAtBefore(CancelStatus status, LocalDateTime createdAt);
}
