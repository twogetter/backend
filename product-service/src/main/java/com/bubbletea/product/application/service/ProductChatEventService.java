package com.bubbletea.product.application.service;


import com.bubbletea.product.domain.event.ProductEventPublisher;
import com.bubbletea.product.domain.event.ProductRegisteredEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductChatEventService {

    private final ProductEventPublisher productEventPublisher;

    public void productRegistered(String artistId, LocalDateTime registeredAt) {
        productEventPublisher.publish(new ProductRegisteredEvent(artistId, registeredAt));
    }

}
