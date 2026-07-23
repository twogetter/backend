package com.bubbletea.payment.support.fixture;

import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent;
import java.math.BigDecimal;

public class BillingEventFixture {
    public static BillingEvent.BillingEventBuilder create(PaymentMethod paymentMethod) {
        return BillingEvent.builder()
                .orderId(100L)
                .methodId(paymentMethod.getId())
                .tossOrderId("toss-order-1")
                .currency("KRW")
                .totalAmount(BigDecimal.valueOf(50000));
    }
}
