package com.bubbletea.order.presentation.external.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "구독 주문 생성 요청")
public record SubscriptionCreateRequestDto(
    @Schema(description = "상품(구독권) ID", example = "1")
    @NotNull @Positive Long productId,

    @Schema(description = "결제수단 ID", example = "10")
    @NotNull @Positive Long paymentMethodId
) {

}
