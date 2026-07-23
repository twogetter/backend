package com.bubbletea.payment.repository;

import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {
    @Modifying
    @Transactional
    @Query(value = "UPDATE payment_outbox SET status = 'PROCESSING', processor_id = :processorId, updated_at = :now " +
            "WHERE id IN (SELECT id FROM payment_outbox WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 50)",
            nativeQuery = true)
    int claimPendingEvents(@Param("processorId") String processorId, @Param("now") LocalDateTime now);

    List<PaymentOutbox> findByStatusAndProcessorId(OutboxStatus status, String processorId);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentOutbox p SET p.status = 'PENDING', p.processorId = null " +
            "WHERE p.status = 'PROCESSING' AND p.processorId = :processorId")
    void rollbackMyProcessingToPending(@Param("processorId") String processorId);

    @Modifying
    @Transactional
    @Query("UPDATE PaymentOutbox p " +
            "SET p.status = 'PENDING', p.processorId = null, p.updatedAt = :now " +
            "WHERE p.status = 'PROCESSING' AND p.updatedAt < :thresholdTime")
    int cleanupStaleProcessingEvents(
            @Param("now") LocalDateTime now,
            @Param("thresholdTime") LocalDateTime thresholdTime
    );

}
