package com.bubbletea.product.application.event;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;

import com.bubbletea.product.domain.event.ProductActivationScheduledEvent;
import com.bubbletea.product.domain.event.ProductDeactivationScheduledEvent;
import com.bubbletea.product.domain.event.ProductDeletionScheduledEvent;
import com.bubbletea.product.domain.event.ProductEventPublisher;
import com.bubbletea.product.domain.event.ProductOpenScheduledEvent;
import com.bubbletea.product.domain.event.ProductPriceChangeScheduledEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductScheduleNotificationServiceTest {

    @InjectMocks
    private ProductScheduleNotificationService notificationService;

    @Mock
    private ProductEventPublisher productEventPublisher;

    private static final String IDEMPOTENCY_KEY = "reservation01";
    private static final String PRODUCT_NAME = "[김연옌] 구독권";

    @Nested
    @DisplayName("notifyOpenSchedule() 테스트")
    class NotifyOpenSchedule {

        @Test
        @DisplayName("성공적으로 이벤트를 발행한다.")
        void publishesProductOpenScheduledEvent() {
            // given
            LocalDateTime openDate = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            notificationService.notifyOpenSchedule(IDEMPOTENCY_KEY, PRODUCT_NAME, openDate);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                any(ProductOpenScheduledEvent.class)
            );
        }

        @Test
        @DisplayName("값이 올바르게 담긴다.")
        void publishesEventWithCorrectPayload() {
            // given
            LocalDateTime openDate = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            notificationService.notifyOpenSchedule(IDEMPOTENCY_KEY, PRODUCT_NAME, openDate);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                argThat(event -> {
                    ProductOpenScheduledEvent openEvent = (ProductOpenScheduledEvent) event;
                    return openEvent.productName().equals(PRODUCT_NAME)
                        && openEvent.openDate().equals(openDate);
                })
            );
        }
    }

    @Nested
    @DisplayName("notifyDeactivationSchedule() 테스트")
    class NotifyDeactivationSchedule {

        @Test
        @DisplayName("성공적으로 이벤트를 발행한다.")
        void publishesProductDeactivationScheduledEvent() {
            // given
            LocalDateTime deactivationDate = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            notificationService.notifyDeactivationSchedule(
                IDEMPOTENCY_KEY, PRODUCT_NAME, deactivationDate);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                any(ProductDeactivationScheduledEvent.class)
            );
        }

        @Test
        @DisplayName("값이 올바르게 담긴다.")
        void publishesEventWithCorrectPayload() {
            // given
            LocalDateTime deactivationDate = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            notificationService.notifyDeactivationSchedule(
                IDEMPOTENCY_KEY, PRODUCT_NAME, deactivationDate);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                argThat(event -> {
                    ProductDeactivationScheduledEvent deactivationEvent =
                        (ProductDeactivationScheduledEvent) event;
                    return deactivationEvent.productName().equals(PRODUCT_NAME)
                        && deactivationEvent.deactivationDate().equals(deactivationDate);
                })
            );
        }
    }

    @Nested
    @DisplayName("notifyActivationSchedule() 테스트")
    class NotifyActivationSchedule {

        @Test
        @DisplayName("성공적으로 이벤트를 발행한다.")
        void publishesProductActivationScheduledEvent() {
            // given
            LocalDateTime activationDate = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            notificationService.notifyActivationSchedule(
                IDEMPOTENCY_KEY, PRODUCT_NAME, activationDate);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                any(ProductActivationScheduledEvent.class)
            );
        }

        @Test
        @DisplayName("값이 올바르게 담긴다.")
        void publishesEventWithCorrectPayload() {
            // given
            LocalDateTime activationDate = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            notificationService.notifyActivationSchedule(
                IDEMPOTENCY_KEY, PRODUCT_NAME, activationDate);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                argThat(event -> {
                    ProductActivationScheduledEvent activationEvent =
                        (ProductActivationScheduledEvent) event;
                    return activationEvent.productName().equals(PRODUCT_NAME)
                        && activationEvent.activationDate().equals(activationDate);
                })
            );
        }
    }

    @Nested
    @DisplayName("notifyDeletionSchedule() 테스트")
    class NotifyDeletionSchedule {

        @Test
        @DisplayName("성공적으로 이벤트를 발행한다.")
        void publishesProductDeletionScheduledEvent() {
            // given
            LocalDateTime deletionDate = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            notificationService.notifyDeletionSchedule(
                IDEMPOTENCY_KEY, PRODUCT_NAME, deletionDate);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                any(ProductDeletionScheduledEvent.class)
            );
        }

        @Test
        @DisplayName("값이 올바르게 담긴다.")
        void publishesEventWithCorrectPayload() {
            // given
            LocalDateTime deletionDate = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            notificationService.notifyDeletionSchedule(
                IDEMPOTENCY_KEY, PRODUCT_NAME, deletionDate);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                argThat(event -> {
                    ProductDeletionScheduledEvent deletionEvent =
                        (ProductDeletionScheduledEvent) event;
                    return deletionEvent.productName().equals(PRODUCT_NAME)
                        && deletionEvent.deletionDate().equals(deletionDate);
                })
            );
        }
    }

    @Nested
    @DisplayName("notifyPriceChangeSchedule() 테스트")
    class NotifyPriceChangeSchedule {

        @Test
        @DisplayName("성공적으로 이벤트를 발행한다.")
        void publishesProductPriceChangeScheduledEvent() {
            // given
            LocalDateTime priceChangeDate = LocalDateTime.of(2026, 7, 23, 12, 0);
            long originalPrice = 10000L;
            long changedPrice = 15000L;

            // when
            notificationService.notifyPriceChangeSchedule(
                IDEMPOTENCY_KEY, PRODUCT_NAME, priceChangeDate, originalPrice, changedPrice);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                any(ProductPriceChangeScheduledEvent.class)
            );
        }

        @Test
        @DisplayName("값이 올바르게 담긴다.")
        void publishesEventWithCorrectPayload() {
            // given
            LocalDateTime priceChangeDate = LocalDateTime.of(2026, 7, 23, 12, 0);
            long originalPrice = 10000L;
            long changedPrice = 15000L;

            // when
            notificationService.notifyPriceChangeSchedule(
                IDEMPOTENCY_KEY, PRODUCT_NAME, priceChangeDate, originalPrice, changedPrice);

            // then
            then(productEventPublisher).should().publish(
                eq(IDEMPOTENCY_KEY),
                argThat(event -> {
                    ProductPriceChangeScheduledEvent priceChangeEvent =
                        (ProductPriceChangeScheduledEvent) event;
                    return priceChangeEvent.productName().equals(PRODUCT_NAME)
                        && priceChangeEvent.priceChangeDate().equals(priceChangeDate)
                        && priceChangeEvent.originalPrice() == originalPrice
                        && priceChangeEvent.changedPrice() == changedPrice;
                })
            );
        }
    }
}