package com.bubbletea.product.infrastructure.event;

import com.bubbletea.product.domain.event.ProductEvent;
import com.bubbletea.product.domain.event.ProductEventPublisher;
import com.bubbletea.product.domain.outbox.OutboxEvent;
import com.bubbletea.product.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProductEventPublisher implements ProductEventPublisher {

    private static final String HEADER_DOMAIN = "domainName";
    private static final String DOMAIN_NAME = "product";
    private static final String HEADER_IDEMPOTENCY_KEY = "idempotencyKey";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    @Override
    public void publish(String idempotencyKey, ProductEvent event) {
        String topic = event.eventType().topic();
        String payload = serialize(event);

        Map<String, String> headers = Map.of(
            HEADER_DOMAIN,
            DOMAIN_NAME,
            HEADER_IDEMPOTENCY_KEY,
            idempotencyKey
        );

        try {
            outboxEventRepository.save(OutboxEvent.of(idempotencyKey, topic, payload, headers));
            log.info("[outbox 적재 완료] idempotencyKey={}, topic={}", idempotencyKey, topic);
        } catch (DuplicateKeyException e) {
            log.info("[outbox 적재 스킵] idempotencyKey={}", idempotencyKey);
        }
    }

    private String serialize(ProductEvent event) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("이벤트 직렬화 실패: " + event, e);
        }
    }
}
