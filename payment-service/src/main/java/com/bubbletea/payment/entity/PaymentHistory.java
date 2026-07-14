package com.bubbletea.payment.entity;

import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.common.BaseEntity;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private PaymentStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus currentStatus;

    private String errorCode;
    private String errorMessage;

    public static PaymentHistory createSuccessHistory(Payment payment, PaymentStatus previousStatus) {
        PaymentHistory history = new PaymentHistory();
        history.payment = payment;
        history.previousStatus = previousStatus;
        history.currentStatus = PaymentStatus.PAID;
        return history;
    }

    public static PaymentHistory createFailHistory(Payment payment, PaymentStatus previousStatus, String errorCode, String errorMessage) {
        PaymentHistory history = new PaymentHistory();
        history.payment = payment;
        history.previousStatus = previousStatus;
        history.currentStatus = PaymentStatus.FAILED;
        history.errorCode = errorCode;
        history.errorMessage = errorMessage;
        return history;
    }
}
