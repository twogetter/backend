package com.bubbletea.payment.controller;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.payment.service.BrandpayService;
import com.bubbletea.payment.service.dto.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private final BrandpayService brandpayService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<PaymentMethodResponseDto>>> getPaymentMethods(@PathVariable Long userId) {
        List<PaymentMethodResponseDto> response = brandpayService.getPaymentMethods(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

