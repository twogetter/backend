package com.bubbletea.payment.service.dto;

//import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.enums.PaymentMethodType;
import com.bubbletea.payment.entity.enums.PaymentMethodStatus;
import lombok.Builder;

@Builder
public record PaymentMethodResponseDto(
        Long id,
        String provider,
        PaymentMethodType type,
        String displayName,
        String maskedNumber,
        Boolean isDefault,
        PaymentMethodStatus status
) {

//    public static PaymentMethodResponseDto of(PaymentMethod paymentMethod) {
//        return PaymentMethodResponseDto.builder()
//                .id(paymentMethod.getId())
//                .provider(paymentMethod.getProvider())
//                .type(paymentMethod.getType())
//                .displayName(paymentMethod.getDisplayName())
//                .maskedNumber(paymentMethod.getMaskedNumber())
//                .isDefault(paymentMethod.getIsDefault())
//                .status(paymentMethod.getStatus())
//                .build();
//    }
}
