package com.bubbletea.payment.service.dto;

import com.bubbletea.payment.entity.PaymentMethod;
import java.util.List;
import lombok.Builder;

public record BrandpayAuthDetailResponseDto(
        Long userId,
        String customerKey,
        List<PaymentMethodDto> cards,
        boolean billingAgreed,
        String clientKey,
        String redirectUrl
) {
    public BrandpayAuthDetailResponseDto(Long userId, String customerKey, List<PaymentMethodDto> cards) {
        this(userId, customerKey, cards, false, null, null);
    }

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
