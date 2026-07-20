package com.bubbletea.order.presentation.external.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SubscriptionCreateRequestDto(
    @NotNull @Positive Long productId,
    @NotNull @Positive Long paymentMethodId
) {

}
