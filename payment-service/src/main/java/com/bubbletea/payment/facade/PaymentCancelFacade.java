package com.bubbletea.payment.facade;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.global.exception.PaymentTossApiException;
import com.bubbletea.payment.service.PaymentCancelService;
import com.bubbletea.payment.service.dto.PaymentCancelRequestDto;
import com.bubbletea.payment.service.dto.PaymentCancelResponseDto;
import com.bubbletea.payment.service.dto.data.PaymentCancelData;
import com.bubbletea.payment.service.external.TossBrandpayApiClient;
import com.bubbletea.payment.service.processor.PaymentPostProcessor;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCancelFacade {

    private final TossBrandpayApiClient tossBrandpayApiClient;
    private final PaymentCancelService paymentCancelService;
    private final PaymentPostProcessor paymentPostProcessor;


    public void cancel(Long userId, PaymentCancelRequestDto dto) {
        log.info("결제 취소 요청 시작 - PaymentId: {}, CancelAmount: {}", dto.paymentId(), dto.cancelAmount());

        PaymentCancelData data = paymentCancelService.readyCancel(userId, dto);

        if(data == null) {
            return;
        }

        try {
            PaymentCancelResponseDto response = tossBrandpayApiClient.cancelPayment(
                    data.paymentKey(),
                    data.idempotencyKey(),
                    data.cancelAmount(),
                    data.cancelReason()
            );


            paymentPostProcessor.completeCancelPayment(data.paymentCancelId(), response.paymentKey(), data.cancelAmount());


        } catch (PaymentTossApiException e) {
            // 8. 네트워크/타임아웃 오류 처리
            if (e.getErrorCode() == PaymentErrorCode.EXTERNAL_SERVER_ERROR) {
                log.warn("토스 최종 승인 타임아웃 또는 서버 에러 발생 - 상태 유지(PENDING) 및 추후 확인 필요: {}", dto.paymentId());
                paymentPostProcessor.holdCancelPayment(data.paymentCancelId(), e.getErrorCode(), e.getMessage());
            } else {
                log.error("토스 결제 승인 거절 (4xx): {}", e.getMessage());
                paymentPostProcessor.failCancelPayment(data.paymentCancelId(), e.getErrorCode(), e.getMessage());
                throw e;
            }
        }catch (PaymentSystemException e) {
            log.error("결제 취소 중 예상치 못한 오류 발생 - PaymentId: {}", dto.paymentId(), e);
            paymentPostProcessor.failCancelPayment(data.paymentCancelId(), e.getErrorCode(), e.getMessage());
            throw e;
        }
    }
}
