package com.bubbletea.payment.entity;

import com.bubbletea.payment.entity.enums.OutboxStatus;
import com.bubbletea.payment.entity.enums.PaymentEventType;
import com.bubbletea.payment.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 도메인 종류 (예: "PAYMENT")
    @Column(nullable = false)
    private String aggregateType;

    // 결제 ID
    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentEventType topic;


    @Column(nullable = false)
    private String messageKey;

    // 카프카에 보낼 실제 메시지 바디 (JSON 데이터)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    // 처리 상태 (PENDING, PROCESSED, FAILED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;


    public void changeStatus(OutboxStatus outboxStatus) {
        this.status = outboxStatus;
    }
}
