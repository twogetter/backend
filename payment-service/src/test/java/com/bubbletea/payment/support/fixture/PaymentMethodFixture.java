package com.bubbletea.payment.support.fixture;

import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.entity.enums.PaymentMethodStatus;
import com.bubbletea.payment.entity.enums.PaymentMethodType;

public class PaymentMethodFixture {
    public static PaymentMethod.PaymentMethodBuilder create(UserBrandpayAuth auth) {
        return PaymentMethod.builder()
                .userBrandpayAuth(auth)
                .provider("toss")
                .type(PaymentMethodType.BILLING)
                .tossMethodId("card-1")
                .tossMethodKey("method-key-1")
                .displayName("주카드")
                .maskedNumber("1234")
                .status(PaymentMethodStatus.ACTIVE)
                .isDefault(true);
    }
}
