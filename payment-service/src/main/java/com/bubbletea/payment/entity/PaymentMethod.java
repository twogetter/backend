package com.bubbletea.payment.entity;

import com.bubbletea.payment.entity.enums.PaymentMethodType;
import com.bubbletea.payment.entity.enums.PaymentMethodStatus;
import com.bubbletea.payment.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_methods")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PaymentMethod extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
//    private String billingKey;
    private String provider;

    @Enumerated(EnumType.STRING)
    private PaymentMethodType type;
    private String tossMethodId;
    private String displayName;
    private String maskedNumber;

    @Enumerated(EnumType.STRING)
    private PaymentMethodStatus status;
    private Boolean isDefault;
}
