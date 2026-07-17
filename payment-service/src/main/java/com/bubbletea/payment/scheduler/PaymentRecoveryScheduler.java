package com.bubbletea.payment.scheduler;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.enums.CancelStatus;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.repository.PaymentCancelRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.service.PaymentService;
import com.bubbletea.payment.service.dto.TossStatusResponseDto;
import com.bubbletea.payment.service.dto.TossStatusResponseDto.TossCancel;
import com.bubbletea.payment.service.external.TossBrandpayApiClient;
import com.bubbletea.payment.service.processor.PaymentPostProcessor;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentRecoveryScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentCancelRepository paymentCancelRepository;
    private final TossBrandpayApiClient tossBrandpayApiClient;
    private final PaymentPostProcessor paymentPostProcessor;


    @Scheduled(fixedDelay = 600000)
    public void recoverAllHoldTransactions() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        List<Payment> holdPayments = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.UNKNOWN_HOLD, threshold);
        for (Payment payment : holdPayments) {
            recoverSinglePayment(payment);
        }

        List<PaymentCancel> holdCancels = paymentCancelRepository.findByStatusAndCreatedAtBefore(CancelStatus.UNKNOWN_HOLD, threshold);
        for (PaymentCancel cancel : holdCancels) {
            recoverSingleCancel(cancel);
        }
    }

    public void recoverSinglePayment(Payment payment) {
        try {
            TossStatusResponseDto response = tossBrandpayApiClient.getTossPaymentStatus(payment.getPaymentKey());

            if ("DONE".equals(response.status())) {
                paymentPostProcessor.completePayment(payment.getId(), payment.getPaymentKey());
                log.info("✅ 결제 복구 성공: 주문 ID {} -> SUCCESS 변경 완료", payment.getOrderId());
            } else if ("FAILED".equals(response.status())) {
                paymentPostProcessor.failPayment(payment.getId(), PaymentErrorCode.TOSS_API_ERROR, "결제 실패 상태로 확인됨");
                log.info("❌ 결제 복구 실패 확정: 주문 ID {} -> FAILED 변경 완료", payment.getOrderId());
            } else {
                if (payment.getCreatedAt().isBefore(LocalDateTime.now().minusHours(1))) {
                    paymentPostProcessor.failPayment(payment.getId(), PaymentErrorCode.TOSS_API_ERROR, "결제 확인 기한 만료 처리");
                    log.warn("⏳ 결제 복구 만료 처리: 주문 ID {} -> 무한 루프 방지를 위해 FAILED 강제 변경", payment.getOrderId());
                }
            }
        } catch (Exception e) {
            log.error("⚠️ 결제 UNKNOWN_HOLD 상태 조회 실패 (다음 루프 재시도). 주문 ID: {}", payment.getOrderId(), e);
        }
    }

    public void recoverSingleCancel(PaymentCancel cancel) {
        try {
            TossStatusResponseDto response = tossBrandpayApiClient.getTossPaymentStatus(cancel.getPayment().getPaymentKey());
            boolean isThisCancelSuccess = false;

            if (response.cancels() != null) {
                for (TossCancel tossCancel : response.cancels()) {
                    if (cancel.getIdempotencyKey().equals(tossCancel.idempotencyKey())) {
                        isThisCancelSuccess = true;
                        break;
                    }
                }
            }

            if (isThisCancelSuccess) {
                paymentPostProcessor.completeCancelPayment(cancel.getId(), cancel.getPayment().getPaymentKey(), cancel.getCancelAmount().longValue());
                log.info("✅ 부분 취소 사후 복구 성공: 취소 ID {} -> CANCEL_SUCCESS 변경 완료", cancel.getId());
            } else {
                if (cancel.getCreatedAt().isBefore(LocalDateTime.now().minusHours(1))) {
                    paymentPostProcessor.failCancelPayment(cancel.getId(), PaymentErrorCode.TOSS_API_ERROR, "실제 취소가 안 되었음 (기한 만료)");
                    log.info("❌ 부분 취소 사후 복구 실패 확정: 실제 취소가 안 되었으므로 취소 ID {} -> CANCEL_FAILED 변경 완료", cancel.getId());
                }
            }
        } catch (Exception e) {
            log.error("⚠️ 취소 상태 조회 실패. 다음 루프 재시도. 취소 ID: {}", cancel.getId(), e);
        }
    }
}
