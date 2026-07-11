package com.bubbletea.payment.controller;

import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.repository.UserBrandpayAuthRepository;
import com.bubbletea.payment.service.BrandpayService;
import com.bubbletea.payment.service.dto.ConnectBrandpayRequestDto;
import com.bubbletea.payment.service.dto.ConnectBrandpayResponseDto;
import com.bubbletea.payment.service.dto.TossBillingRequestDto;
import com.bubbletea.payment.service.dto.TossWebhookRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/brandpay")
public class TossBrandpayController {

    private final BrandpayService brandpayService;
    private final UserBrandpayAuthRepository userBrandpayAuthRepository;

    @PostMapping("/webhooks/toss-brandpay")
    public ResponseEntity<String> handleTossWebhook(
            @RequestBody TossWebhookRequestDto request,
            @RequestHeader(value = "TossPayments-Signature", required = false) String signature)
            throws Exception {
        String eventType = request.eventType();

        if ("METHOD_UPDATED".equals(eventType)) {
            String customerKey = request.data().customerKey();

            UserBrandpayAuth userBrandpayAuth = userBrandpayAuthRepository.findByCustomerKey(customerKey).orElseThrow();
            brandpayService.syncPaymentMethods(userBrandpayAuth);
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
    public ResponseEntity<?> billingSuccess(
            @RequestBody TossBillingRequestDto request) {
        brandpayService.billingAllow(request.customerKey());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/billing-auth/terminate")
    public ResponseEntity<String> terminateBillingAuth(@RequestBody TossBillingRequestDto request) {
        brandpayService.terminateBilling(request.customerKey());
        return ResponseEntity.ok("success");
    }


}
