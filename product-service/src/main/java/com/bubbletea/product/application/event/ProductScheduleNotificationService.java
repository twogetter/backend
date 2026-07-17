package com.bubbletea.product.application.event;

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

    public void notifyOpenSchedule(
        String idempotencyKey, String productName, LocalDateTime openDate) {
        productEventPublisher.publish(idempotencyKey,
            new ProductOpenScheduledEvent(productName, openDate)
        );
    }

    public void notifyDeactivationSchedule(
        String idempotencyKey, String productName, LocalDateTime deactivationDate) {
        productEventPublisher.publish(idempotencyKey,
            new ProductDeactivationScheduledEvent(productName, deactivationDate));
    }

    public void notifyActivationSchedule(
        String idempotencyKey, String productName, LocalDateTime activationDate) {
        productEventPublisher.publish(idempotencyKey,
            new ProductActivationScheduledEvent(productName, activationDate));
    }

    public void notifyDeletionSchedule(
        String idempotencyKey, String productName, LocalDateTime deletionDate) {
        productEventPublisher.publish(idempotencyKey,
            new ProductDeletionScheduledEvent(productName, deletionDate));
    }

    public void notifyPriceChangeSchedule(
        String idempotencyKey,
        String productName, LocalDateTime priceChangeDate,
        long originalPrice, long changedPrice
    ) {
        productEventPublisher.publish(idempotencyKey,
            new ProductPriceChangeScheduledEvent(
                productName, priceChangeDate, originalPrice, changedPrice
            )
        );
    }
}
