package com.bubbletea.payment.controller;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.facade.PaymentConfirmFacade;
import com.bubbletea.payment.service.BrandpayService;
import com.bubbletea.payment.service.dto.ConnectBrandpayRequestDto;
import com.bubbletea.payment.service.dto.ConnectBrandpayResponseDto;
import com.bubbletea.payment.service.dto.PaymentReadyRequestDto;
import com.bubbletea.payment.service.dto.TossBillingChangeStatusRequestDto;
import com.bubbletea.payment.service.dto.TossWebhookRequestDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/brandpay")
public class TossBrandpayController {

    private final BrandpayService brandpayService;
private final PaymentConfirmFacade paymentConfirmFacade;

    @PostMapping("/payments/ready")
    public ResponseEntity<Map<String, String>> readyPayment(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody PaymentReadyRequestDto dto) {
        String tossMethodId = paymentConfirmFacade.ready(dto, userId);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "tossMethodId", tossMethodId
        ));
    }

    @PostMapping("/webhooks/toss-brandpay")
    public ResponseEntity<String> handleTossWebhook(
            @RequestBody TossWebhookRequestDto request,
            @RequestHeader(value = "TossPayments-Signature", required = false) String signature)
            throws Exception {
        String eventType = request.eventType();

        if ("METHOD_UPDATED".equals(eventType)) {
            String customerKey = request.data().customerKey();

            brandpayService.syncPaymentMethods(customerKey);
        } else if ("CUSTOMER_STATUS_CHANGED".equals(eventType)) {
            String status = request.data().status();
            String customerKey = request.data().customerKey();
            if ("REMOVED".equals(status)) {
                brandpayService.disconnectBrandpayByWebhook(customerKey);
                log.info("disconnected success!!");
            }
        }

        return ResponseEntity.ok("OK");
    }

    @GetMapping("/callback-auth")
    @CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true" )
    public ResponseEntity<?> callbackAuth(
            @RequestParam Long userId,
            @RequestParam String customerKey,
            @RequestParam String code) {
        try {
            ConnectBrandpayResponseDto response = brandpayService.connectBrandpay(
                    ConnectBrandpayRequestDto.builder()
                            .userId(userId)
                            .customerKey(customerKey)
                            .code(code)
                            .build()
            );
            return ResponseEntity.ok(response);
        } catch (PaymentSystemException e) {
            log.error("PaymentException in callbackAuth", e);
            JSONObject error = new JSONObject();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    @PostMapping("/billing-auth/success")
    public ApiResponse<String> billingSuccess(
            @RequestBody TossBillingChangeStatusRequestDto request) {
        brandpayService.billingAllow(request.customerKey());
        return ApiResponse.success("정기결제 인증 상태가 반영되었습니다.");
    }

    @PostMapping("/billing-auth/terminate")
    public ApiResponse<String> terminateBillingAuth(@RequestBody TossBillingChangeStatusRequestDto request) {
        brandpayService.terminateBilling(request.customerKey());
        return ApiResponse.success("정기 자동결제가 해지되었습니다.");
    }


}
