package com.bubbletea.order.presentation.external.dto;

public record SubscriptionCreateRequestDto(
    Long productId,
    Long paymentMethodId
) {

}
