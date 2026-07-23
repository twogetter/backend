package com.bubbletea.payment.processor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.commontest.container.KafkaTestContainer;
import com.bubbletea.commontest.container.PostgresTestContainer;
import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import com.bubbletea.payment.entity.enums.PaymentEventType;
import com.bubbletea.payment.infrastructure.kafka.dto.PaymentResultEvent;
import com.bubbletea.payment.repository.PaymentOutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentOutboxProcessorIntegrationTest implements PostgresTestContainer, KafkaTestContainer {

    @Autowired
    private PaymentOutboxProcessor paymentOutboxProcessor;
    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        paymentOutboxRepository.deleteAll();
    }

    @Test
    @DisplayName("미발행 아웃박스는 카프카로 전송되고 PROCESSED로 전환된다")
    void postEventPublishesKafkaMessage() throws Exception {
        // given
        String topic = "payment.outbox.it." + UUID.randomUUID();
        PaymentResultEvent event = new PaymentResultEvent(
                901L,
                10L,
                BigDecimal.valueOf(12000L),
                "paykey-901",
                "PaymentSucceeded",
                ""
        );

        PaymentOutbox outbox = paymentOutboxRepository.save(
                PaymentOutbox.builder()
                        .aggregateType("payment")
                        .aggregateId(10L)
                        .topic(topic)
                        .eventType(PaymentEventType.SUCCESS.getEventTypeHeader())
                        .messageKey("901")
                        .payload(objectMapper.writeValueAsString(event))
                        .status(OutboxStatus.PENDING)
                        .build()
        );

        try (KafkaConsumer<String, String> consumer = createConsumer(topic)) {
            // when
            paymentOutboxProcessor.postEvent();

            ConsumerRecord<String, String> record = awaitRecord(consumer, topic);

            // then
            PaymentResultEvent actual = objectMapper.readValue(record.value(), PaymentResultEvent.class);
            assertThat(actual).isEqualTo(event);
            assertThat(record.key()).isEqualTo("901");
            assertThat(new String(record.headers().lastHeader("X-Domain").value()))
                    .isEqualTo("payment");
            assertThat(new String(record.headers().lastHeader("X-Event-Type").value()))
                    .isEqualTo(PaymentEventType.SUCCESS.getEventTypeHeader());
            assertThat(paymentOutboxRepository.findById(outbox.getId()).orElseThrow().getStatus())
                    .isEqualTo(OutboxStatus.PROCESSED);
        }
    }

    @Test
    @DisplayName("PROCESSING 상태가 오래 지속된 아웃박스는 안전하게 PENDING으로 복구된다")
    void cleanEventRestoresStaleProcessingRow() {
        // given
        PaymentOutbox outbox = paymentOutboxRepository.save(
                PaymentOutbox.builder()
                        .aggregateType("payment")
                        .aggregateId(11L)
                        .topic("payment.outbox.cleanup")
                        .eventType(PaymentEventType.SUCCESS.getEventTypeHeader())
                        .messageKey("cleanup-11")
                        .payload("{\"status\":\"PaymentSucceeded\"}")
                        .processorId("processor-1")
                        .status(OutboxStatus.PROCESSING)
                        .build()
        );
        LocalDateTime staleTime = LocalDateTime.now().minusMinutes(10);
        Timestamp timestamp = Timestamp.valueOf(staleTime);
        jdbcTemplate.update("update payment_outbox set created_at = ?, updated_at = ? where id = ?",
                timestamp, timestamp, outbox.getId());

        // when
        paymentOutboxProcessor.cleanEvent();

        // then
        PaymentOutbox persisted = paymentOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(persisted.getProcessorId()).isNull();
    }

    private KafkaConsumer<String, String> createConsumer(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-outbox-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(topic));
        consumer.poll(Duration.ofMillis(100));
        return consumer;
    }

    private ConsumerRecord<String, String> awaitRecord(KafkaConsumer<String, String> consumer, String topic) {
        for (int i = 0; i < 50; i++) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, String> record : records.records(topic)) {
                return record;
            }
        }
        throw new AssertionError("카프카 메시지를 수신하지 못했습니다: " + topic);
    }
}
