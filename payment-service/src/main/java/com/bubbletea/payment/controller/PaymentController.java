package com.bubbletea.payment.controller;

import com.bubbletea.payment.service.PaymentService;
import com.bubbletea.payment.service.dto.PaymentConfirmRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public String paymentConfirm(PaymentConfirmRequestDto dto) {
        paymentService.confirm(dto);
        return "Payment confirmed";
    }
}
