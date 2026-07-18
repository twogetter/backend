package com.bubbletea.payment.service;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.service.dto.data.BillingConfirmData;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BillingConfirmData createReadyPayment(Long userId, Long orderId, BillingEvent event) {

        PaymentMethod paymentMethod = paymentMethodRepository.findById(event.methodId())
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_METHOD_NOT_FOUND));

        if(!Objects.equals(paymentMethod.getUserBrandpayAuth().getUserId(), userId)) {
            throw new PaymentSystemException(PaymentErrorCode.PAYMENT_METHOD_NOT_FOUND);
        }

//        if (paymentRepository.existsByOrderId(orderId)) {
//            throw new AlreadyProcessedException("이미 존재하는 주문건입니다.");
//        }

        UserBrandpayAuth auth = paymentMethod.getUserBrandpayAuth();
        String idempotencyKey = "payment-confirm-" + event.orderId() + "-" + UUID.randomUUID();

        Payment payment = Payment.builder()
                .userId(userId)
                .orderId(orderId)
                .tossOrderId(event.tossOrderId())
                .totalAmount(event.totalAmount())
                .currency(event.currency())
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.READY)
                .idempotencyKey(idempotencyKey)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        return BillingConfirmData.builder()
                .paymentId(savedPayment.getId())
                .customerKey(auth.getCustomerKey())
                .methodKey(paymentMethod.getTossMethodKey())
                .idempotencyKey(savedPayment.getIdempotencyKey())
                .build();

    }


}
