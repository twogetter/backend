package com.bubbletea.payment.controller;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.facade.PaymentConfirmFacade;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.service.PaymentService;
import com.bubbletea.payment.service.dto.PaymentConfirmRequestDto;
import com.bubbletea.payment.service.dto.PaymentReadyRequestDto;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class PaymentController {

    private final PaymentConfirmFacade paymentConfirmFacade;


    @PostMapping("/confirm/brandpay") //등록 다건 결제
    public ApiResponse<String> confirmPayment(@RequestBody PaymentConfirmRequestDto dto) {
        paymentConfirmFacade.confirm(dto);
        return ApiResponse.success(dto.amount() + "원 결제가 완료되었습니다.");
    }

//    @PostMapping(path = {"/confirm/widget", "/confirm/payment"}) // 비등록 단건 결제
//    public ApiResponse<String> confirmWidgetPayment(@RequestBody PaymentConfirmRequestDto dto) {
//        paymentService.confirm(dto);
//        return ApiResponse.success(dto.amount() + "원 결제가 완료되었습니다.");
//    }
}
