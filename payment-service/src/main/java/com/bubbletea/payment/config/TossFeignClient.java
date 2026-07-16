package com.bubbletea.payment.config;

import com.bubbletea.payment.service.dto.PaymentConfirmRequestDto;
import com.bubbletea.payment.service.dto.PaymentConfirmResponseDto;
import com.bubbletea.payment.service.dto.PaymentCancelResponseDto;
import com.bubbletea.payment.service.dto.TossAccessTokenResponseDto;
import com.bubbletea.payment.service.dto.BillingRequestDto;
import com.bubbletea.payment.service.dto.TossRegisteredPaymentMethodsResponseDto;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "tossBrandpayClient", url = "https://api.tosspayments.com/v1")
public interface TossFeignClient {

    @PostMapping("/brandpay/authorizations/access-token")
    TossAccessTokenResponseDto getAccessToken(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Object> requestData
    );

    @GetMapping("/brandpay/payments/methods")
    TossRegisteredPaymentMethodsResponseDto getRegisteredPaymentMethods(
            @RequestHeader("Authorization") String authorization
    );

    @PostMapping("/brandpay/payments/confirm")
    PaymentConfirmResponseDto confirmBrandpay(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentConfirmRequestDto dto
    );

    @PostMapping("/payments/confirm")
    PaymentConfirmResponseDto confirmPayment(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "TossPayments-Test-Code", required = false) String testCode,
            @RequestBody PaymentConfirmRequestDto dto
    );

    @PostMapping("/brandpay/payments")
    PaymentConfirmResponseDto executeBilling(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
//            @RequestHeader(value = "TossPayments-Test-Code", required = false) String testCode,
            @RequestBody BillingRequestDto dto
    );

    @PostMapping("/payments/{paymentKey}/cancel")
    PaymentCancelResponseDto cancelPayment(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable String paymentKey,
            @RequestBody Map<String, Object> requestData
    );

}
