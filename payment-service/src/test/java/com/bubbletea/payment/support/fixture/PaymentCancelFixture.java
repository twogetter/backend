package com.bubbletea.payment.support.fixture;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.enums.CancelStatus;
import java.math.BigDecimal;

public class PaymentCancelFixture {

    public static PaymentCancel.PaymentCancelBuilder create(Payment payment) {
        BigDecimal cancelAmount = payment.getRefundableAmount() != null
                ? payment.getRefundableAmount()
                : payment.getTotalAmount();

        return PaymentCancel.builder()
                .payment(payment)
                .cancelType("PAYMENT_CANCEL")
                .cancelAmount(cancelAmount)
                .cancelReason("사용자 요청")
                .idempotencyKey("cancel-" + payment.getId() + "-idemp")
                .status(CancelStatus.UNKNOWN_HOLD);
    }
}
