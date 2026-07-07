package com.bubbletea.payment.controller;

import com.bubbletea.payment.service.PaymentMethodService;
import com.bubbletea.payment.service.dto.PaymentMethodResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping("/{userId}")
    public List<PaymentMethodResponseDto> getPaymentMethods(@PathVariable Long userId) {
        return paymentMethodService.getPaymentMethods(userId);
//        return ApiResponse.success(paymentMethods);
    }
}
