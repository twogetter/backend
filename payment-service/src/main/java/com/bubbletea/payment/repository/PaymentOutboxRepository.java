package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE payment_outbox SET status = 'PROCESSING' " +
            "WHERE id IN (SELECT id FROM payment_outbox WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 50)",
            nativeQuery = true)
    int claimPendingEvents();

    List<PaymentOutbox> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentOutbox p SET p.status = 'PENDING' WHERE p.status = 'PROCESSING'")
    void rollbackAllProcessingToPending();
}
