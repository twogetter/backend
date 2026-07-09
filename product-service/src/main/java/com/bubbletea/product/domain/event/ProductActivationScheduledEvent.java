package com.bubbletea.product.domain.event;

import java.time.LocalDateTime;


public record ProductActivationScheduledEvent(
        String productName,
        LocalDateTime activationDate
) implements ProductEvent {

    @Override
    public ProductEventType eventType() {
        return ProductEventType.PRODUCT_ACTIVATION_SCHEDULE;
    }

}
