package com.bubbletea.product.domain.event;

import java.time.LocalDateTime;


public record ProductDeletionScheduledEvent(
        String productName,
        LocalDateTime deletionDate
) implements ProductEvent {

    @Override
    public ProductEventType eventType() {
        return ProductEventType.PRODUCT_DELETION_SCHEDULE;
    }
}
