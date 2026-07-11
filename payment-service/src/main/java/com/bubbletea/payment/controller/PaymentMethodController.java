package com.bubbletea.payment.controller;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.payment.service.BrandpayService;
import com.bubbletea.payment.service.dto.*;
import com.bubbletea.payment.service.PaymentMethodService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private final BrandpayService brandpayService;
    private final PaymentMethodService paymentMethodService;

    @GetMapping("/{userId}")
    public ApiResponse<List<PaymentMethodResponseDto>> getPaymentMethods(@PathVariable Long userId) {
        List<PaymentMethodResponseDto> response = brandpayService.getPaymentMethods(userId);
        return ApiResponse.success(response);
    }

    @PostMapping("/{paymentMethodId}/billing")
    public ApiResponse<?> registerBillingMethod(
            @RequestHeader(name = "X-User-Id") Long userId,
                    @PathVariable Long paymentMethodId
            // @AuthenticationPrincipal UserPrincipal user
    ) {
        log.info("▶ 정기결제 카드 지정 요청 - 유저ID: {}, 지정할 카드 Key: {}", userId, paymentMethodId);
            paymentMethodService.updatePaymentMethodBilling(paymentMethodId, userId);
            return ApiResponse.success("정기결제 카드가 성공적으로 지정되었습니다.");
    }
}

