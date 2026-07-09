package com.bubbletea.product.domain.event;


public interface ProductEventPublisher {
    void publish(ProductEvent event);
}
