package com.bubbletea.product.domain.event;

import java.time.LocalDateTime;


public record ProductRegisteredEvent(
    Long artistId,
    LocalDateTime registeredAt
) implements ProductEvent {

    @Override
    public ProductEventType eventType() {
        return ProductEventType.PRODUCT_REGISTERED;
    }

}
