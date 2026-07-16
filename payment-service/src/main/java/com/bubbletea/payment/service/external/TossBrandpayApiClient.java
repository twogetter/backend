package com.bubbletea.payment.service.external;

import com.bubbletea.payment.config.TossFeignClient;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.global.exception.PaymentTossApiException;
import com.bubbletea.payment.service.dto.PaymentConfirmRequestDto;
import com.bubbletea.payment.service.dto.PaymentConfirmResponseDto;
import com.bubbletea.payment.service.dto.PaymentCancelResponseDto;
import com.bubbletea.payment.service.dto.TossAccessTokenResponseDto;
import com.bubbletea.payment.service.dto.TossBillingChangeStatusRequestDto;
import com.bubbletea.payment.service.dto.BillingRequestDto;
import com.bubbletea.payment.service.dto.TossRegisteredPaymentMethodsResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossBrandpayApiClient {

    private static final String BRANDPAY_API_BASE_URL = "https://api.tosspayments.com/v1";

    @Value("${toss.payments.api-secret-key}")
    private String apiSecretKey;

    private final TossFeignClient tossFeignClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TossAccessTokenResponseDto getAccessToken(String customerKey, String code) {
        String encodedToken = getBasicAuthHeader();

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("grantType", "AuthorizationCode");
        requestData.put("customerKey", customerKey);
        requestData.put("code", code);

        log.info("Toss API Request: getAccessToken");
        try {
            return tossFeignClient.getAccessToken(encodedToken, requestData);
        } catch (FeignException e) {
            try {
                return objectMapper.readValue(e.contentUTF8(), TossAccessTokenResponseDto.class);
            } catch (Exception ex) {
                return new TossAccessTokenResponseDto(null, null, "HTTP_ERROR", e.getMessage());
            }
        }
    }

    public TossRegisteredPaymentMethodsResponseDto getRegisteredPaymentMethods(String accessToken) {
        String bearerToken = "Bearer " + accessToken;

        log.info("Toss API Request: getRegisteredPaymentMethods");
        try {
            return tossFeignClient.getRegisteredPaymentMethods(bearerToken);
        } catch (FeignException e) {
            try {
                return objectMapper.readValue(e.contentUTF8(), TossRegisteredPaymentMethodsResponseDto.class);
            } catch (Exception ex) {
                return new TossRegisteredPaymentMethodsResponseDto(null, null, null, "HTTP_ERROR", e.getMessage());
            }
        }
    }

    @CircuitBreaker(name = "brandpayCircuitBreaker", fallbackMethod = "confirmBrandpayFallback")
    public PaymentConfirmResponseDto confirmBrandpay(PaymentConfirmRequestDto dto, String idempotencyKey) {
        String encodedToken = getBasicAuthHeader();
//        String encodedToken = "err";

        log.info("Brandpay Confirm Request - OrderId: {}, Idempotency: {}", dto.orderId(), idempotencyKey);

        try {
//            EXPIRED_CARD, REJECT_CARD_PAYMENT, INSUFFICIENT_BALANCE,
//            CONFIRM_TIMEOUT, INTERNAL_SERVER_ERROR
//            String testErrorTriggerCode = "INTERNAL_SERVER_ERROR";

            return tossFeignClient.confirmBrandpay(encodedToken, idempotencyKey, dto);
        } catch (FeignException e) {
            if (e.status() >= 500 || e.status() == 429) {
                log.warn("토스 API 일시적 오류. 재시도를 트리거합니다. Status: {}", e.status());
                throw e;
            }
            log.error("Brandpay Confirm Client Error (4xx): {}", e.contentUTF8());
            throw new PaymentTossApiException(PaymentErrorCode.TOSS_API_ERROR);
        } catch (Exception e) {
            throw new PaymentSystemException(PaymentErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    public PaymentConfirmResponseDto confirmBrandpayFallback(PaymentConfirmRequestDto dto, String idempotencyKey, Throwable t) {
        if (t instanceof CallNotPermittedException) {
            log.error("🚨 [Fast-Fail] 토스 서버 장애 지속으로 인해 서킷 브레이커가 요청을 즉시 차단함. OrderId: {}", dto.orderId());
        } else {

            log.error("단건 결제 최종 실패 - OrderId: {}, Cause: {}", dto.orderId(), t.getMessage());
        }
        throw new PaymentTossApiException(PaymentErrorCode.EXTERNAL_SERVER_ERROR);
    }

    @Retry(name = "billingRetry", fallbackMethod = "executeBillingFallback")
    public PaymentConfirmResponseDto executeBilling(
            Long paymentId,
            String idempotencyKey,
            BillingRequestDto dto
    ) {
        String encodedToken = getBasicAuthHeader();

        log.info("Toss Billing Request - OrderId: {}, Idempotency: {}, PaymentId: {}",
                dto.orderId(), idempotencyKey, paymentId);

        try {
            //            EXPIRED_CARD, REJECT_CARD_PAYMENT, INSUFFICIENT_BALANCE,
//            CONFIRM_TIMEOUT, INTERNAL_SERVER_ERROR
//            TIMEOUT, REJECT_CARD_COMPANY, INSUFFICIENT_FUNDS
//            String testErrorTriggerCode = "REJECT_CARD_COMPANY";
//            return tossFeignClient.executeBilling(encodedToken, idempotencyKey,testErrorTriggerCode, dto);
            return tossFeignClient.executeBilling(encodedToken, idempotencyKey, dto);

        } catch (FeignException e) {
            if (e.status() >= 500 || e.status() == 429 || e.status() == 408) {
                log.warn("토스 빌링 API 일시적 오류 또는 타임아웃. 재시도를 트리거합니다. Status: {}", e.status());
                throw e;
            }

            // 2. 재시도 불가 대상: 4xx 클라이언트 에러 (한도초과, 잔액부족, 카드만료 등)
            log.error("Toss Billing Client Error (4xx) - 상태 확정 실패 처리: {}", e.contentUTF8());
            throw new PaymentTossApiException(PaymentErrorCode.TOSS_API_ERROR);

        } catch (Exception e) {
            log.error("Toss Billing System Error - 알 수 없는 내부 예외 발생", e);
            throw new PaymentSystemException(PaymentErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    public PaymentConfirmResponseDto executeBillingFallback(
            Long paymentId,
            String idempotencyKey,
            BillingRequestDto dto,
            FeignException e
    ) {
        log.error("정기결제 최종 재시도 실패 또는 타임아웃 발생 (Fallback 진입) - OrderId: {}, Idempotency: {}, Reason: {}",
                dto.orderId(), idempotencyKey, e.getMessage());

        throw new PaymentTossApiException(PaymentErrorCode.EXTERNAL_SERVER_ERROR);
    }

    @Retry(name = "cancelRetry", fallbackMethod = "cancelPaymentFallback")
    public PaymentCancelResponseDto cancelPayment(
            String paymentKey,
            String idempotencyKey,
            Long cancelAmount,
            String cancelReason
    ) {
        String encodedToken = getBasicAuthHeader();

        log.info("Toss Payment Cancel Request - PaymentKey: {}, Idempotency: {}, CancelAmount: {}",
    paymentKey, idempotencyKey, cancelAmount);

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("cancelReason", cancelReason);
        if (cancelAmount != null) {
            requestData.put("cancelAmount", cancelAmount);
        }

        try {
            return tossFeignClient.cancelPayment(encodedToken, idempotencyKey, paymentKey, requestData);
        } catch (FeignException e) {
            if (e.status() >= 500 || e.status() == 429 || e.status() == 408) {
                log.warn("토스 결제 취소 API 일시적 오류 또는 타임아웃. 재시도를 트리거합니다. Status: {}", e.status());
                throw e;
            }

            log.error("Toss Cancel Client Error (4xx): {}", e.contentUTF8());
            throw new PaymentTossApiException(PaymentErrorCode.TOSS_API_ERROR);
        } catch (Exception e) {
            log.error("Toss Cancel System Error - 알 수 없는 내부 예외 발생", e);
            throw new PaymentSystemException(PaymentErrorCode.UNAUTHORIZED_ACCESS);
        }
    }

    public PaymentCancelResponseDto cancelPaymentFallback(
            String paymentKey,
            String idempotencyKey,
            Long cancelAmount,
            String cancelReason,
            FeignException e
    ) {
        log.error("결제 취소 최종 재시도 실패 또는 타임아웃 발생 (Fallback 진입) - PaymentKey: {}, Reason: {}",
                paymentKey, e.getMessage());
        throw new PaymentTossApiException(PaymentErrorCode.EXTERNAL_SERVER_ERROR);
    }

    private String getBasicAuthHeader() {
        String rawToken = apiSecretKey + ":";
        String encodedToken = Base64.getEncoder().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedToken;
    }

//    public PaymentConfirmResponseDto confirmPayment(PaymentConfirmRequestDto dto, String idempotencyKey) {
//        String url = BRANDPAY_API_BASE_URL + "/payments/confirm";
//
//        String rawToken = apiSecretKey + ":";
//        String encodedToken = Base64.getEncoder().encodeToString(rawToken.getBytes(StandardCharsets.UTF_8));
//
//        try {
//            return restClient.post()
//                    .uri(url)
//                    .headers(headers -> {
//                        headers.set("Authorization", "Basic " + encodedToken);
//                        headers.setContentType(MediaType.APPLICATION_JSON);
//                        headers.set("Idempotency-Key", idempotencyKey);
//                    })
//                    .body(dto)
//                    .retrieve()
//                    .body(PaymentConfirmResponseDto.class);
//        } catch (RestClientResponseException e) {
//            String errorBody = e.getResponseBodyAsString();
//            throw new PaymentTossApiException(PaymentErrorCode.TOSS_API_ERROR);
//        } catch (Exception e) {
//            //TODO: 잔액부족같은 만료 같은 에러 분기처리 필요
//            throw new PaymentSystemException(PaymentErrorCode.UNAUTHORIZED_ACCESS);
//        }
//    }


}
