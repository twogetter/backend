package com.bubbletea.product.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.bubbletea.product.domain.event.ProductEventType;
import com.bubbletea.product.domain.event.ProductRegisteredEvent;
import com.bubbletea.product.domain.outbox.OutboxEvent;
import com.bubbletea.product.domain.outbox.OutboxEventRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
public class OutboxProductEventPublisherTest {

    @InjectMocks
    private OutboxProductEventPublisher publisher;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private JsonMapper jsonMapper;

    private static final String IDEMPOTENCY_KEY = "product-1:ARTIST_REGISTERED";
    private static final Long ARTIST_ID = 1L;
    private static final LocalDateTime REGISTERED_AT = LocalDateTime.of(2026, 7, 24, 0, 0);

    private OutboxEvent createOutboxEvent(String payload) {
        return OutboxEvent.of(
            IDEMPOTENCY_KEY,
            ProductEventType.PRODUCT_REGISTERED.topic(),
            payload,
            Map.of("domainName", "product", "idempotencyKey", IDEMPOTENCY_KEY)
        );
    }

    @Nested
    @DisplayName("publish 테스트")
    class Publish {

        @Test
        @DisplayName("정보가 성공적으로 저장된다.")
        void savesOutboxEventWithCorrectTopicAndHeaders() {
            // given
            ProductRegisteredEvent event = new ProductRegisteredEvent(ARTIST_ID, REGISTERED_AT);
            String serializedPayload = "{\"artistId\":1}";

            given(jsonMapper.writeValueAsString(event)).willReturn(serializedPayload);
            given(outboxEventRepository.save(argThat(outboxEvent ->
                outboxEvent.getIdempotencyKey().equals(IDEMPOTENCY_KEY)
            ))).willReturn(createOutboxEvent(serializedPayload));

            // when
            publisher.publish(IDEMPOTENCY_KEY, event);

            // then
            then(outboxEventRepository).should().save(
                argThat(outboxEvent ->
                    outboxEvent.getTopic().equals(
                        ProductEventType.PRODUCT_REGISTERED.topic()) &&
                        outboxEvent.getPayload().equals(serializedPayload) &&
                        outboxEvent.getIdempotencyKey().equals(IDEMPOTENCY_KEY) &&
                        "product".equals(outboxEvent.getHeaders().get("domainName")) &&
                        IDEMPOTENCY_KEY.equals(outboxEvent.getHeaders().get("idempotencyKey"))
                )
            );
        }

        @Test
        @DisplayName("중복된 키가 있을 경우 저장 없이 스킵된다.")
        void duplicateKeyException_skipsWithoutThrowing() {
            // given
            ProductRegisteredEvent event = new ProductRegisteredEvent(ARTIST_ID, REGISTERED_AT);
            String serializedPayload = "{\"artistId\":1}";

            given(jsonMapper.writeValueAsString(event)).willReturn(serializedPayload);
            willThrow(new DuplicateKeyException("duplicate idempotencyKey"))
                .given(outboxEventRepository).save(any(OutboxEvent.class));

            // when & then
            publisher.publish(IDEMPOTENCY_KEY, event);

            then(outboxEventRepository).should().save(any(OutboxEvent.class));
        }

        @Test
        @DisplayName("직렬화 실패 시 예외가 던져지고 저장되지 않는다.")
        void serializationFails_throwsIllegalStateException() {
            // given
            ProductRegisteredEvent event = new ProductRegisteredEvent(ARTIST_ID, REGISTERED_AT);

            given(jsonMapper.writeValueAsString(event))
                .willThrow(new RuntimeException("직렬화 오류"));

            // when & then
            assertThatThrownBy(() -> publisher.publish(IDEMPOTENCY_KEY, event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이벤트 직렬화 실패");

            then(outboxEventRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("topic은 event의 eventType으로 결정된다.")
        void topicIsResolvedFromEventType() {
            // given
            ProductRegisteredEvent event = new ProductRegisteredEvent(ARTIST_ID, REGISTERED_AT);
            String expectedTopic = ProductEventType.PRODUCT_REGISTERED.topic();
            String serializedPayload = "{\"artistId\":1}";

            given(jsonMapper.writeValueAsString(event)).willReturn(serializedPayload);
            given(outboxEventRepository.save(any(OutboxEvent.class)))
                .willReturn(createOutboxEvent(serializedPayload));

            // when
            publisher.publish(IDEMPOTENCY_KEY, event);

            // then
            then(outboxEventRepository).should().save(
                argThat(outboxEvent -> outboxEvent.getTopic().equals(expectedTopic))
            );
        }
    }

}
