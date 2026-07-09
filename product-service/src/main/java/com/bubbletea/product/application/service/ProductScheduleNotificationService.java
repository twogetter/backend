package com.bubbletea.product.application.service;

import com.bubbletea.product.domain.event.ProductActivationScheduledEvent;
import com.bubbletea.product.domain.event.ProductDeactivationScheduledEvent;
import com.bubbletea.product.domain.event.ProductDeletionScheduledEvent;
import com.bubbletea.product.domain.event.ProductEventPublisher;
import com.bubbletea.product.domain.event.ProductOpenScheduledEvent;
import com.bubbletea.product.domain.event.ProductPriceChangeScheduledEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class ProductScheduleNotificationService {

    private final ProductEventPublisher productEventPublisher;

    public void notifyOpenSchedule(String productName, LocalDateTime openDate) {
        productEventPublisher.publish(new ProductOpenScheduledEvent(productName, openDate));
    }

    public void notifyDeactivationSchedule(String productName, LocalDateTime deactivationDate) {
        productEventPublisher.publish(new ProductDeactivationScheduledEvent(productName, deactivationDate));
    }

    public void notifyActivationSchedule(String productName, LocalDateTime activationDate) {
        productEventPublisher.publish(new ProductActivationScheduledEvent(productName, activationDate));
    }

    public void notifyDeletionSchedule(String productName, LocalDateTime deletionDate) {
        productEventPublisher.publish(new ProductDeletionScheduledEvent(productName, deletionDate));
    }

    public void notifyPriceChangeSchedule(
        String productName, LocalDateTime priceChangeDate,
        int originalPrice, int changedPrice
    ) {
        productEventPublisher.publish(
            new ProductPriceChangeScheduledEvent(
                productName, priceChangeDate, originalPrice, changedPrice
            )
        );
    }
}
