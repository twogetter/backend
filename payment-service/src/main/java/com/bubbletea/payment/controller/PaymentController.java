package com.bubbletea.payment.controller;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.payment.facade.PaymentCancelFacade;
import com.bubbletea.payment.facade.PaymentConfirmFacade;
import com.bubbletea.payment.service.dto.PaymentCancelRequestDto;
import com.bubbletea.payment.service.dto.PaymentConfirmRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentConfirmFacade paymentConfirmFacade;
    private final PaymentCancelFacade paymentCancelFacade;

    @PostMapping("/confirm/brandpay")
    public ApiResponse<String> confirmPayment(@RequestBody PaymentConfirmRequestDto dto) {
        paymentConfirmFacade.confirm(dto);
        return ApiResponse.success(dto.amount() + "원 결제가 완료되었습니다.");
    }

    @PostMapping("/cancel")
    public ApiResponse<String> requestCancel(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody PaymentCancelRequestDto dto) {
        paymentCancelFacade.cancel(userId, dto, false);
        return ApiResponse.success(dto.cancelAmount() + "원 결제 취소가 완료되었습니다.");
    }

//    @PostMapping(path = {"/confirm/widget", "/confirm/payment"}) // 비등록 단건 결제
//    public ApiResponse<String> confirmWidgetPayment(@RequestBody PaymentConfirmRequestDto dto) {
//        paymentService.confirm(dto);
//        return ApiResponse.success(dto.amount() + "원 결제가 완료되었습니다.");
//    }
}
