package com.bubbletea.product.application.event;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;

import com.bubbletea.product.domain.event.ProductEventPublisher;
import com.bubbletea.product.domain.event.ProductRegisteredEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductChatEventServiceTest {

    @InjectMocks
    private ProductChatEventService productChatEventService;

    @Mock
    private ProductEventPublisher productEventPublisher;

    @Nested
    @DisplayName("productRegistered() 테스트")
    class ProductRegistered {

        @Test
        @DisplayName("성공적으로 이벤트를 발행한다.")
        void publishesProductRegisteredEvent() {
            // given
            String idempotencyKey = "product-1:ARTIST_REGISTERED";
            Long artistId = 1L;
            LocalDateTime registeredAt = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            productChatEventService.productRegistered(idempotencyKey, artistId, registeredAt);

            // then
            then(productEventPublisher).should().publish(
                eq(idempotencyKey),
                any(ProductRegisteredEvent.class)
            );
        }

        @Test
        @DisplayName("값이 올바르게 담긴다.")
        void publishesEventWithCorrectPayload() {
            // given
            String idempotencyKey = "product-1:ARTIST_REGISTERED";
            Long artistId = 42L;
            LocalDateTime registeredAt = LocalDateTime.of(2026, 7, 23, 12, 0);

            // when
            productChatEventService.productRegistered(idempotencyKey, artistId, registeredAt);

            // then
            then(productEventPublisher).should().publish(
                eq(idempotencyKey),
                argThat(event -> {
                    ProductRegisteredEvent registeredEvent = (ProductRegisteredEvent) event;
                    return registeredEvent.artistId().equals(artistId)
                        && registeredEvent.registeredAt().equals(registeredAt);
                })
            );
        }
    }
}