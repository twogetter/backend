package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.enums.CancelStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, Long> {
    Optional<PaymentCancel> findByIdempotencyKey(String idempotencyKey);

    @Query("select pc from PaymentCancel pc join fetch pc.payment where pc.status = :status and pc.createdAt < :threshold")
    List<PaymentCancel> findByStatusAndCreatedAtBefore( @Param("status") CancelStatus status,
                                                        @Param("threshold") LocalDateTime threshold);
}
