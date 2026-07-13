package com.bubbletea.product.domain.event;

import java.time.LocalDateTime;


public record ProductPriceChangeScheduledEvent(
    String productName,
    LocalDateTime priceChangeDate,
    long originalPrice,
    long changedPrice
) implements ProductEvent {

    @Override
    public ProductEventType eventType() {
        return ProductEventType.PRODUCT_PRICE_CHANGE_SCHEDULE;
    }

}
