package com.bubbletea.product.domain.event;

import java.time.LocalDateTime;


public record ProductDeactivationScheduledEvent(
        String productName,
        LocalDateTime deactivationDate
) implements ProductEvent {

    @Override
    public ProductEventType eventType() {
        return ProductEventType.PRODUCT_DEACTIVATION_SCHEDULE;
    }
}
