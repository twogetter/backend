package com.bubbletea.product.domain.event;


public interface ProductEventPublisher {

    void publish(String idempotencyKey, ProductEvent event);
}
