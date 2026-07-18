package com.bubbletea.payment.processor;

import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import com.bubbletea.payment.infrastructure.kafka.dto.PaymentResultEvent;
import com.bubbletea.payment.repository.PaymentOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventProcessor {
    private final PaymentOutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String HEADER_DOMAIN = "X-Domain";
    private static final String HEADER_EVENT_TYPE = "X-Event-Type";
    private static final String HEADER_EVENT_TIMESTAMP = "X-Event-Timestamp";

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToKafkaAndUpdateStatus(PaymentOutbox outbox) throws Exception {
        String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        PaymentResultEvent eventDto = objectMapper.readValue(outbox.getPayload(), PaymentResultEvent.class);

        for (String targetTopic : outbox.getTopic().getTopics()) {

            Message<PaymentResultEvent> message = MessageBuilder
                    .withPayload(eventDto)
                    .setHeader(KafkaHeaders.TOPIC, targetTopic)
                    .setHeader(KafkaHeaders.KEY, outbox.getMessageKey())
                    .setHeader(HEADER_DOMAIN, outbox.getAggregateType())
                    .setHeader(HEADER_EVENT_TYPE, outbox.getTopic().getEventTypeHeader())
                    .setHeader(HEADER_EVENT_TIMESTAMP, currentTimestamp)
                    .build();
            kafkaTemplate.send(message).get();

            log.info("[Outbox Scheduler] 토픽 개별 발행 성공 -> Topic: {}", targetTopic);
        }

        outbox.changeStatus(OutboxStatus.PROCESSED);
        outboxRepository.save(outbox);

        log.info("[Outbox Scheduler] 카프카 발행 및 DB 업데이트 완료. ID: {}, Topic: {}", outbox.getId(), outbox.getTopic());
    }

}
