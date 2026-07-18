package com.bubbletea.payment.scheduler;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.enums.CancelStatus;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.facade.PaymentCancelFacade;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.repository.PaymentCancelRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.service.dto.PaymentCancelRequestDto;
import com.bubbletea.payment.service.dto.TossStatusResponseDto;
import com.bubbletea.payment.service.external.TossBrandpayApiClient;
import com.bubbletea.payment.processor.PaymentPostProcessor;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRecoveryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;
    private final TossBrandpayApiClient tossBrandpayApiClient;
    private final PaymentPostProcessor paymentPostProcessor;
    private final PaymentCancelFacade paymentCancelFacade;


    @Scheduled(fixedDelay = 600000)
    public void recoverAllHoldTransactions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        List<Payment> holdPayments = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.UNKNOWN_HOLD, threshold);
        for (Payment payment : holdPayments) {
            recoverSinglePayment(payment);
        }

        List<PaymentCancel> holdCancels = paymentCancelRepository.findByStatusAndCreatedAtBefore(CancelStatus.UNKNOWN_HOLD, threshold);
        for (PaymentCancel cancel : holdCancels) {
            try {
                paymentCancelFacade.cancel(cancel.getPayment().getUserId(), PaymentCancelRequestDto.builder()
                        .paymentId(cancel.getPayment().getId())
                        .cancelAmount(cancel.getCancelAmount())
                        .cancelReason(cancel.getCancelReason())
                        .idempotencyKey(cancel.getIdempotencyKey())
                        .build(), true);
            } catch (Exception e) {
                log.error("⚠️ 결제 취소 UNKNOWN_HOLD 상태 처리 실패 (다음 루프 재시도). 주문 ID: {}, 원인: {}",
                        cancel.getPayment().getOrderId(), e.getMessage(), e);
            }
        }
    }

    public void recoverSinglePayment(Payment payment) {
        try {
            TossStatusResponseDto response = tossBrandpayApiClient.getTossPaymentStatus(payment.getPaymentKey());

            if ("DONE".equals(response.status())) {
                paymentPostProcessor.completePayment(payment.getId(), payment.getPaymentKey());
                log.info("✅ 결제 복구 성공: 주문 ID {} -> SUCCESS 변경 완료", payment.getOrderId());
            } else if ("FAILED".equals(response.status())) {
                paymentPostProcessor.failPayment(payment.getId(), PaymentErrorCode.TOSS_PAYMENT_REJECTED,
                        "결제 실패 상태로 확인됨");
                log.info("❌ 결제 복구 실패 확정: 주문 ID {} -> FAILED 변경 완료", payment.getOrderId());
            } else {
                if (payment.getCreatedAt().isBefore(LocalDateTime.now().minusHours(1))) {
                    paymentPostProcessor.failPayment(payment.getId(), PaymentErrorCode.TOSS_PAYMENT_REJECTED,
                            "결제 확인 기한 만료 처리");
                    log.warn("⏳ 결제 복구 만료 처리: 주문 ID {} -> 무한 루프 방지를 위해 FAILED 강제 변경", payment.getOrderId());
                }
            }
        } catch (Exception e) {
            log.error("⚠️ 결제 UNKNOWN_HOLD 상태 조회 실패 (다음 루프 재시도). 주문 ID: {}", payment.getOrderId(), e);
        }
    }
}
