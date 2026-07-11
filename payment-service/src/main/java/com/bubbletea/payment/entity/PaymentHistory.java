package com.bubbletea.payment.entity;

import com.bubbletea.payment.global.common.BaseEntity;
import jakarta.persistence.Entity;
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

    private String previousStatus;
    private String currentStatus;
//
//    @Enumerated(EnumType.STRING)
//    private PaymentStatus previousStatus;
//
//    @Enumerated(EnumType.STRING)
//    private PaymentStatus currentStatus;

    private String errorCode;
    private String errorMessage;
}
