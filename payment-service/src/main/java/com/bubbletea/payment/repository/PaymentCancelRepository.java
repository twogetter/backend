package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.enums.CancelStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {
    Optional<PaymentCancel> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"payment"})
    List<PaymentCancel> findTop100ByStatusAndCreatedAtBefore(
            CancelStatus status,
            LocalDateTime threshold
    );
}
