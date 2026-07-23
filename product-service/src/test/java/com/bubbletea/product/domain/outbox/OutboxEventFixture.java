package com.bubbletea.product.domain.outbox;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.test.util.ReflectionTestUtils;

public class OutboxEventFixture {

    public static final String DEFAULT_ID = "outbox-event-1";
    public static final String DEFAULT_IDEMPOTENCY_KEY = "idempotency-key-1";
    public static final String DEFAULT_TOPIC = "product.registered";
    public static final String DEFAULT_PAYLOAD = "{\"productId\":\"product-1\"}";
    public static final Map<String, String> DEFAULT_HEADERS = Map.of("type", "ProductRegistered");
    public static final LocalDateTime DEFAULT_CLAIMED_AT =
        LocalDateTime.of(2026, 7, 23, 12, 0);
    
    public static OutboxEvent create() {
        OutboxEvent event = OutboxEvent.of(
            DEFAULT_IDEMPOTENCY_KEY,
            DEFAULT_TOPIC,
            DEFAULT_PAYLOAD,
            DEFAULT_HEADERS
        );
        injectBaseFields(event, DEFAULT_ID, 0);
        return event;
    }

    public static OutboxEvent createWithRetryCount(int retryCount) {
        OutboxEvent event = OutboxEvent.of(
            DEFAULT_IDEMPOTENCY_KEY,
            DEFAULT_TOPIC,
            DEFAULT_PAYLOAD,
            DEFAULT_HEADERS
        );
        injectBaseFields(event, DEFAULT_ID, retryCount);
        return event;
    }

    public static OutboxEvent create(String id, String topic, int retryCount) {
        OutboxEvent event = OutboxEvent.of(
            DEFAULT_IDEMPOTENCY_KEY + "-" + id,
            topic,
            DEFAULT_PAYLOAD,
            DEFAULT_HEADERS
        );
        injectBaseFields(event, id, retryCount);
        return event;
    }

    private static void injectBaseFields(
        OutboxEvent event, String id, int retryCount
    ) {
        ReflectionTestUtils.setField(event, "id", id);
        ReflectionTestUtils.setField(event, "claimedAt", OutboxEventFixture.DEFAULT_CLAIMED_AT);
        ReflectionTestUtils.setField(event, "retryCount", retryCount);
    }

    private OutboxEventFixture() {
    }
}
