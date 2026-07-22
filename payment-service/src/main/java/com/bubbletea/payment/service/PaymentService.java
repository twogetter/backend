package com.bubbletea.payment.service;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.service.dto.PaymentReadyRequestDto;
import com.bubbletea.payment.service.dto.data.PaymentConfirmData;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Transactional
    public String createReadyPayment(PaymentReadyRequestDto dto, Long userId) {

        PaymentMethod paymentMethod = paymentMethodRepository.findById(dto.selectedMethodId())
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_METHOD_NOT_FOUND));

        //TODO: 주무도메인과 통신 후 검증 필요

        if (!Objects.equals(userId, paymentMethod.getUserBrandpayAuth().getUserId())) {
            throw new PaymentSystemException(PaymentErrorCode.PAYMENT_METHOD_NOT_FOUND);
        }

        Payment existingPayment = paymentRepository.findByTossOrderId(dto.tossOrderId()).orElse(null);

        if(existingPayment != null &&
                Objects.equals(existingPayment.getUserId(), userId)) {
            return existingPayment.getPaymentMethod().getTossMethodId();
        }

        String idempotencyKey = "payment-confirm-" + dto.orderId() + "-" + UUID.randomUUID();

        Payment payment = Payment.builder()
                .userId(userId)
                .orderId(dto.orderId())
                .tossOrderId(dto.tossOrderId())
                .totalAmount(new BigDecimal(dto.amount()))
                .currency(dto.currency())
                .paymentMethod(paymentMethod)
                .status(PaymentStatus.READY)
                .idempotencyKey(idempotencyKey)
                .build();

        paymentRepository.save(payment);

        return paymentMethod.getTossMethodId();
    }

    @Transactional(readOnly = true)
    public PaymentConfirmData getConfirmData(String tossOrderId) {
        Payment payment = paymentRepository.findByTossOrderId(tossOrderId)
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        if(payment.getStatus() != PaymentStatus.READY) {
            throw new PaymentSystemException(PaymentErrorCode.PAYMENT_NOT_READY);
        }
        return new PaymentConfirmData(payment.getId(), payment.getIdempotencyKey(), payment.getTotalAmount());
    }
}
