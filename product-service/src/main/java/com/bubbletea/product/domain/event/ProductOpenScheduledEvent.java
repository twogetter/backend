package com.bubbletea.product.domain.event;

import java.time.LocalDateTime;


public record ProductOpenScheduledEvent(
    String productName,
    LocalDateTime openDate
) implements ProductEvent {

    @Override
    public ProductEventType eventType() {
        return ProductEventType.PRODUCT_OPEN_SCHEDULE;
    }
}
