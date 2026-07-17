package com.bubbletea.payment.service.processor;

import com.bubbletea.common.exception.ErrorCode;
import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.PaymentHistory;
import com.bubbletea.payment.entity.enums.CancelStatus;
import com.bubbletea.payment.entity.enums.HistoryType;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.infrastructure.kafka.PaymentEventPublisher;
import com.bubbletea.payment.infrastructure.kafka.dto.PaymentResultEvent;
import com.bubbletea.payment.repository.PaymentCancelRepository;
import com.bubbletea.payment.repository.PaymentHistoryRepository;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPostProcessor {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PaymentCancelRepository paymentCancelRepository;

    @Transactional
    public void completePayment(Long paymentId, String paymentKey) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        PaymentStatus previousStatus = payment.getStatus();

        payment.complete(paymentKey, PaymentStatus.PAID);

        PaymentHistory history = PaymentHistory.createSuccessHistory(payment, previousStatus, PaymentStatus.PAID, HistoryType.PAYMENT_REQUEST);
        paymentHistoryRepository.save(history);

        PaymentResultEvent event = new PaymentResultEvent(
                payment.getOrderId(),
                paymentId,
                payment.getTotalAmount(),
                paymentKey,
                "PaymentSucceeded",
                ""
        );
        paymentEventPublisher.publishPaymentSuccess(event);

    }

    @Transactional
    public void failPayment(Long paymentId, ErrorCode errorCode, String errorMessage) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        PaymentStatus previousStatus = payment.getStatus();
        payment.changeStatus(PaymentStatus.FAILED);

        PaymentHistory history = PaymentHistory.createFailHistory(payment, previousStatus, PaymentStatus.FAILED, HistoryType.PAYMENT_REQUEST, errorCode.toString(),
                errorMessage);
        paymentHistoryRepository.save(history);

        PaymentResultEvent event = new PaymentResultEvent(
                payment.getOrderId(),
                paymentId,
                payment.getTotalAmount(),
                "",
                "PaymentFailed",
                errorMessage
        );
        paymentEventPublisher.publishPaymentFailed(event);
    }

    @Transactional
    public void holdPayment(Long paymentId, ErrorCode errorCode, String errorMessage) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_NOT_FOUND));

        PaymentStatus previousStatus = payment.getStatus();
        payment.changeStatus(PaymentStatus.UNKNOWN_HOLD);

        PaymentHistory history = PaymentHistory.createFailHistory(payment, previousStatus, PaymentStatus.UNKNOWN_HOLD, HistoryType.PAYMENT_REQUEST, errorCode.toString(),
                errorMessage);
        paymentHistoryRepository.save(history);

        PaymentResultEvent event = new PaymentResultEvent(
                payment.getOrderId(),
                paymentId,
                payment.getTotalAmount(),
                "",
                "PaymentUnknownEvent",
                errorMessage
        );
        paymentEventPublisher.publishPaymentHold(event);
    }

    @Transactional
    public void completeCancelPayment(Long paymentCancelId, String paymentKey, Long cancelAmount) {
        PaymentCancel paymentCancel = paymentCancelRepository.findById(paymentCancelId)
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_CANCEL_NOT_FOUND));

        Payment payment = paymentCancel.getPayment();

        PaymentStatus previousStatus = payment.getStatus();

        payment.complete(paymentKey, PaymentStatus.CANCELLED);
        paymentCancel.changeStatus(CancelStatus.SUCCESS);

        BigDecimal updatedRefundableAmount = payment.getRefundableAmount()
                .subtract(BigDecimal.valueOf(cancelAmount));
        payment.updateRefundableAmount(updatedRefundableAmount);

        PaymentHistory history = PaymentHistory.createSuccessHistory(payment, previousStatus, PaymentStatus.CANCELLED, HistoryType.CANCEL_REQUEST);
        paymentHistoryRepository.save(history);

        PaymentResultEvent event = new PaymentResultEvent(
                payment.getOrderId(),
                payment.getId(),
                payment.getTotalAmount(),
                paymentKey,
                "PaymentCancelled",
                ""
        );
        paymentEventPublisher.publishCancelPaymentSuccess(event);

    }

    @Transactional
    public void failCancelPayment(Long paymentCancelId, ErrorCode errorCode, String errorMessage) {
        PaymentCancel paymentCancel = paymentCancelRepository.findById(paymentCancelId)
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_CANCEL_NOT_FOUND));

        Payment payment = paymentCancel.getPayment();

        PaymentStatus previousStatus = payment.getStatus();
        paymentCancel.changeStatus(CancelStatus.FAILED);

        PaymentHistory history = PaymentHistory.createFailHistory(payment, previousStatus, PaymentStatus.FAILED, HistoryType.CANCEL_REQUEST, errorCode.toString(),
                errorMessage);
        paymentHistoryRepository.save(history);

        PaymentResultEvent event = new PaymentResultEvent(
                payment.getOrderId(),
                payment.getId(),
                payment.getTotalAmount(),
                "",
                "PaymentCancelFailed",
                errorMessage
        );
        paymentEventPublisher.publishCancelPaymentFailed(event);
    }

    @Transactional
    public void holdCancelPayment(Long paymentCancelId, ErrorCode errorCode, String errorMessage) {
        PaymentCancel paymentCancel = paymentCancelRepository.findById(paymentCancelId)
                .orElseThrow(() -> new PaymentSystemException(PaymentErrorCode.PAYMENT_CANCEL_NOT_FOUND));

        Payment payment = paymentCancel.getPayment();

        PaymentStatus previousStatus = payment.getStatus();
        payment.changeStatus(PaymentStatus.CANCEL_UNKNOWN_HOLD);
        paymentCancel.changeStatus(CancelStatus.UNKNOWN_HOLD);

        PaymentHistory history = PaymentHistory.createFailHistory(payment, previousStatus, PaymentStatus.CANCEL_UNKNOWN_HOLD, HistoryType.CANCEL_REQUEST, errorCode.toString(),
                errorMessage);
        paymentHistoryRepository.save(history);

        PaymentResultEvent event = new PaymentResultEvent(
                payment.getOrderId(),
                payment.getId(),
                payment.getTotalAmount(),
                "",
                "PaymentCancelUnknownEvent",
                errorMessage
        );
        paymentEventPublisher.publishCancelPaymentHold(event);
    }

}
