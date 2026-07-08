package com.bubbletea.product.kafka;

import com.bubbletea.product.domain.event.ProductEvent;
import com.bubbletea.product.domain.event.ProductEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProductEventPublisher implements ProductEventPublisher {

    private static final String HEADER_DOMAIN = "domainName";
    private static final String DOMAIN_NAME = "product";

    private final KafkaTemplate<String, Object> productEventKafkaTemplate;

    @Override
    public void publish(ProductEvent event) {
        String topic = event.eventType().topic();

        Message<ProductEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(HEADER_DOMAIN, DOMAIN_NAME)
                .build();

        productEventKafkaTemplate.send(message).whenComplete((result, ex) -> {
            if (ex != null) {
                // TODO: 발행 실패 시 재시도 정책은 실제 기능 구현 단계에서 추가 예정
                log.error("[Kafka] 이벤트 발행 실패. topic={}, event={}", topic, event, ex);
                return;
            }
            log.info("[Kafka] 이벤트 발행 성공. topic={}, partition={}, offset={}",
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        });
    }

}
