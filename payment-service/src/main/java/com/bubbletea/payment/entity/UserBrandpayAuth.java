package com.bubbletea.payment.entity;

import com.bubbletea.payment.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_brandpay_auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBrandpayAuth extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String customerKey;
    private String accessToken;
    private String refreshToken;
}
