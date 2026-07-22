package com.bubbletea.payment.service.dto;

import com.bubbletea.payment.entity.PaymentMethod;
import java.util.List;
import lombok.Builder;

public record BrandpayAuthDetailResponseDto(
        Long userId,
        String customerKey,
        List<PaymentMethodDto> cards
) {

    @Builder
    public record PaymentMethodDto(
            Long id,
            String displayName,
            String maskedNumber,
            String methodId

    ) {

        public static PaymentMethodDto of (PaymentMethod paymentMethod) {
            return PaymentMethodDto.builder()
                    .id(paymentMethod.getId())
                    .displayName(paymentMethod.getDisplayName())
                    .maskedNumber(paymentMethod.getMaskedNumber())
                    .methodId(paymentMethod.getTossMethodKey())
                    .build();
        }
    }
}
