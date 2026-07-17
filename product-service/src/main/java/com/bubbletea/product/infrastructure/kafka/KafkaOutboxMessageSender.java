package com.bubbletea.product.infrastructure.kafka;

import com.bubbletea.product.domain.outbox.OutboxMessageSender;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class KafkaOutboxMessageSender implements OutboxMessageSender {

    private static final long SEND_TIMEOUT_SECONDS = 5L;

    private final KafkaTemplate<String, String> outboxKafkaTemplate;

    @Override
    public void send(String topic, String payload, Map<String, String> headers) {
        Message<String> message = MessageBuilder
            .withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, topic)
            .copyHeaders(headers)
            .build();

        try {
            outboxKafkaTemplate.send(message).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("[Kafka 발행 중단] topic=" + topic, e);
        } catch (Exception e) {
            throw new IllegalStateException("[Kafka 발행 실패] topic=" + topic, e);
        }
    }
}
