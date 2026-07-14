package com.bubbletea.payment.entity;

import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.common.BaseEntity;
import jakarta.persistence.Column;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    private Long orderId;
    private String tossOrderId;
    private Long userId;
    private String paymentKey;
    private String paymentType;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal refundableAmount;

    private String currency;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String idempotencyKey;

    public void complete(String paymentKey, PaymentStatus status) {
        this.paymentKey = paymentKey;
        this.status = status;
        this.refundableAmount = this.totalAmount;
    }

    public void changeStatus(PaymentStatus status) {
        this.status = status;
    }
}
