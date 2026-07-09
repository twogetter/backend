package com.bubbletea.payment.controller;

import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.repository.UserBrandpayAuthRepository;
import com.bubbletea.payment.service.BrandpayService;
import com.bubbletea.payment.service.dto.TossWebhookRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks/toss-brandpay")
public class TossWebhookController {

    private final BrandpayService brandpayService;
    private final UserBrandpayAuthRepository userBrandpayAuthRepository;

    @PostMapping
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
}
