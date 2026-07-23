package com.bubbletea.payment.support.fixture;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import java.math.BigDecimal;

public class PaymentFixture {
    public static Payment.PaymentBuilder create(Long userId, String tossOrderId, long amount, PaymentMethod method) {
        return Payment.builder()
                .userId(userId)
                .orderId(userId + 100L) // 규칙성 있는 기본값
                .tossOrderId(tossOrderId)
                .totalAmount(BigDecimal.valueOf(amount))
                .currency("KRW")
                .paymentMethod(method)
                .status(PaymentStatus.READY)
                .idempotencyKey("payment-" + userId + "-idemp");
    }


}
