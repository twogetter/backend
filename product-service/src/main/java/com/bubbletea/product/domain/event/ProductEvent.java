package com.bubbletea.product.domain.event;


public sealed interface ProductEvent permits
    ProductRegisteredEvent,
    ProductOpenScheduledEvent,
    ProductDeactivationScheduledEvent,
    ProductActivationScheduledEvent,
    ProductDeletionScheduledEvent,
    ProductPriceChangeScheduledEvent {

    ProductEventType eventType();
}
