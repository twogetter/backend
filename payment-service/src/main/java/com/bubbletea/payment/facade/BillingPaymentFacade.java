package com.bubbletea.payment.facade;

import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.global.exception.PaymentTossApiException;
import com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent;
import com.bubbletea.payment.service.BillingService;
import com.bubbletea.payment.service.dto.BillingRequestDto;
import com.bubbletea.payment.service.dto.PaymentConfirmResponseDto;
import com.bubbletea.payment.service.dto.data.BillingConfirmData;
import com.bubbletea.payment.service.external.TossBrandpayApiClient;
import com.bubbletea.payment.service.processor.PaymentPostProcessor;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillingPaymentFacade {

    private final BillingService billingService;
    private final TossBrandpayApiClient tossBrandpayApiClient; // Spring Cloud OpenFeign
    private final PaymentPostProcessor paymentPostProcessor;

    /**
     * 정기결제 전체 프로세스 관장 (외부 API 호출을 포함하므로 @Transactional은 붙이지 않음)
     */
    public void executeBilling(Long userId, Long orderId, BillingEvent event) {

        BillingConfirmData data;
        try {
            data = billingService.createReadyPayment(userId, orderId, event);
        } catch (DataIntegrityViolationException e) {
            log.warn("이미 처리 중이거나 완료된 정기결제 요청입니다. OrderId: {}", orderId);
            return;
        }

        Long paymentId = data.paymentId();
        BillingRequestDto dto = BillingRequestDto.builder()
                .customerKey(data.customerKey())
                .methodKey(data.methodKey())
                .orderId(event.tossOrderId())
                .orderName(event.orderName())
                .amount(event.totalAmount().longValue())
                .build();

        try {
            // OpenFeign을 통한 1단계 즉시 승인 요청
            PaymentConfirmResponseDto response = tossBrandpayApiClient.executeBilling(
                    paymentId, data.idempotencyKey(), dto
            );

            paymentPostProcessor.completePayment(paymentId, response.paymentKey());

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
