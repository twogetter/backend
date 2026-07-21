package com.bubbletea.payment.support.fixture;

import com.bubbletea.payment.entity.UserBrandpayAuth;

public class UserBrandpayAuthFixture {
    public static UserBrandpayAuth.UserBrandpayAuthBuilder create() {
        return UserBrandpayAuth.builder()
                .userId(1L)
                .customerKey("customer-key-1")
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .isBillingAgreed(true);
    }
}
