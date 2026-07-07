package com.bubbletea.payment.service;

import com.bubbletea.payment.entity.enums.PaymentMethodType;
import com.bubbletea.payment.entity.enums.PaymentMethodStatus;
import com.bubbletea.payment.service.dto.PaymentMethodResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

//    private final PaymentMethodRepository paymentMethodRepository;

    public List<PaymentMethodResponseDto> getPaymentMethods(Long userId) {

        List<PaymentMethodResponseDto> res = List.of(PaymentMethodResponseDto.builder()
                .id(1L)
                .provider("TOSS_BRANDPAY")
                .type(PaymentMethodType.NORMAL)
                .displayName("Personal Card")
                .maskedNumber("1234-****-****-3456")
                .isDefault(true)
                .status(PaymentMethodStatus.ACTIVE)
                .build(),
            PaymentMethodResponseDto.builder()
                .id(2L)
                .provider("TOSS_BRANDPAY")
                .type(PaymentMethodType.BOTH)
                .displayName("Business Card")
                .maskedNumber("9876-****-****-7654")
                .isDefault(false)
                .status(PaymentMethodStatus.EXPIRED)
                .build());
        return res;
//        return paymentMethodRepository.findAllByUserId(userId).stream().map(PaymentMethodResponseDto::of).toList();
    }

}
