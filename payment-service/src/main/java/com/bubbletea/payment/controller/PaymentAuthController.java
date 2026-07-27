
package com.bubbletea.payment.controller;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.payment.service.BrandpayService;
import com.bubbletea.payment.service.dto.BrandpayAuthDetailResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentAuthController {

    private final BrandpayService brandpayService;

    @Value("${toss.payments.client-key}")
    private String CLIENT_KEY;

    @Value("${toss.payments.widget-secret-key}")
    private String WIDGET_SECRET_KEY;

    @Value("${toss.payments.api-secret-key}")
    private String API_SECRET_KEY;

    @Value("${toss.payments.redirect-url}")
    private String REDIRECT_URL;

    /**
     * 사용자의 결제 정보(customerKey, 등록된 카드 등)를 조회
     */
    @GetMapping("/customer-info")
    public ApiResponse<BrandpayAuthDetailResponseDto> getCustomerInfo(
            @RequestHeader("X-User-Id") Long userId) {
        BrandpayAuthDetailResponseDto info = brandpayService.getCustomerKey(userId);
        BrandpayAuthDetailResponseDto response = new BrandpayAuthDetailResponseDto(
                info.userId(),
                info.customerKey(),
                info.cards(),
                info.billingAgreed(),
                CLIENT_KEY,
                REDIRECT_URL
        );
        return ApiResponse.success(response);
    }



}
