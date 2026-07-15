package com.bubbletea.payment.facade;

import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.global.exception.PaymentTossApiException;
import com.bubbletea.payment.service.PaymentService;
import com.bubbletea.payment.service.dto.PaymentConfirmRequestDto;
import com.bubbletea.payment.service.dto.PaymentConfirmResponseDto;
import com.bubbletea.payment.service.dto.PaymentReadyRequestDto;
import com.bubbletea.payment.service.dto.data.PaymentConfirmData;
import com.bubbletea.payment.service.external.OrderApiClient;
import com.bubbletea.payment.service.external.TossBrandpayApiClient;
import com.bubbletea.payment.service.processor.PaymentPostProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConfirmFacade {

    private final PaymentService paymentService;
    private final TossBrandpayApiClient tossBrandpayApiClient;
    private final PaymentPostProcessor paymentPostProcessor;
    private final OrderApiClient orderApiClient;

    public String ready(PaymentReadyRequestDto dto, Long userId) {

        // TODO: 주문 도메인과 Feign 통신으로 주문/금액 검증을 먼저 수행한다.
        return paymentService.createReadyPayment(dto, userId);
    }

    public void confirm(PaymentConfirmRequestDto dto) {
        // 멱등키 고려
        PaymentConfirmData data = paymentService.getConfirmData(dto.orderId());

        try {
            PaymentConfirmResponseDto response = tossBrandpayApiClient.confirmBrandpay(dto, data.idempotencyKey());

            paymentPostProcessor.completePayment(data.paymentId(), response.paymentKey());

        } catch (PaymentTossApiException e) {
            if (e.getErrorCode() == PaymentErrorCode.EXTERNAL_SERVER_ERROR) {
                log.warn("토스 최종 승인 타임아웃 또는 서버 에러 발생 - 상태 유지(PENDING) 및 추후 확인 필요: {}", dto.orderId());
                paymentPostProcessor.holdPayment(data.paymentId(), e.getErrorCode(), e.getMessage());
            } else {
                log.error("토스 결제 승인 거절 (4xx): {}", e.getMessage());
                paymentPostProcessor.failPayment(data.paymentId(), e.getErrorCode(), e.getMessage());
            }
            throw e;
        } catch (PaymentSystemException e) {
            log.error("시스템 에러 발생: {}", e.getMessage());
            paymentPostProcessor.failPayment(data.paymentId(), e.getErrorCode(), e.getMessage());

            throw e;
        }
    }


}
