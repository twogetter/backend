package com.bubbletea.payment.service;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.enums.CancelStatus;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.repository.PaymentCancelRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.service.dto.PaymentCancelRequestDto;
import com.bubbletea.payment.service.dto.data.PaymentCancelData;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCancelService {

    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;

    @Transactional
    public PaymentCancelData readyCancel(Long userId, PaymentCancelRequestDto dto) {
        Payment payment = paymentRepository.findById(dto.paymentId())
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        validatePayment(userId, payment);
        validateCancelAmount(payment, dto.cancelAmount());

        PaymentCancel existingPaymentCancel = paymentCancelRepository.findByIdempotencyKey(dto.idempotencyKey()).orElse(null);
        PaymentCancel paymentCancel;
        if (existingPaymentCancel != null) {
            if (existingPaymentCancel.getStatus() == CancelStatus.SUCCESS) {
                return null;
            }
            if (existingPaymentCancel.getStatus() == CancelStatus.UNKNOWN_HOLD) {
                throw new PaymentSystemException(PaymentErrorCode.CANCEL_UNKNOWN_HOLD);
            }
            paymentCancel = existingPaymentCancel;
            paymentCancel.changeStatus(CancelStatus.REQUEST);
        } else {
            paymentCancel = PaymentCancel.builder()
                    .payment(payment)
                    .cancelAmount(dto.cancelAmount())
                    .cancelReason(dto.cancelReason())
                    .idempotencyKey(dto.idempotencyKey())
                    .status(CancelStatus.REQUEST)
                    .build();
        }

        PaymentCancel savedPaymentCancel = paymentCancelRepository.save(paymentCancel);

        return new PaymentCancelData(
                savedPaymentCancel.getId(),
                savedPaymentCancel.getPayment().getId(),
                savedPaymentCancel.getPayment().getPaymentKey(),
                savedPaymentCancel.getIdempotencyKey(),
                savedPaymentCancel.getCancelAmount().longValue(),
                savedPaymentCancel.getCancelReason()
        );
    }

    private void validatePayment(Long userId, Payment payment) {
        PaymentStatus status = payment.getStatus();

        if(!Objects.equals(payment.getUserId(), userId)) {
            throw new PaymentSystemException(
                    PaymentErrorCode.UNAUTHORIZED_ACCESS,
                    "결제 요청자와 결제 정보의 사용자 ID가 일치하지 않습니다."
            );
        }

        if (status != PaymentStatus.PAID) {
            throw new PaymentSystemException(
                    PaymentErrorCode.INVALID_PAYMENT_STATUS,
                    "취소 불가능한 결제 상태입니다: " + status
            );
        }
    }

    private void validateCancelAmount(Payment payment, BigDecimal cancelAmount) {
        if (cancelAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentSystemException(
                    PaymentErrorCode.INVALID_CANCEL_AMOUNT,
                    "취소 금액은 0보다 커야 합니다"
            );
        }

        if (payment.getRefundableAmount() == null) {
            throw new PaymentSystemException(
                    PaymentErrorCode.PAYMENT_NOT_FOUND,
                    "환불 가능 금액 정보가 없습니다"
            );
        }

        if (cancelAmount.compareTo(payment.getRefundableAmount()) > 0) {
            throw new PaymentSystemException(
                    PaymentErrorCode.INVALID_CANCEL_AMOUNT,
                    "취소 금액이 환불 가능 금액을 초과했습니다. 환불 가능 금액: " + payment.getRefundableAmount()
            );
        }
    }
}
