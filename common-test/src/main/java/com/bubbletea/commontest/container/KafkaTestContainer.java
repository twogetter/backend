package com.bubbletea.commontest.container;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.ConfluentKafkaContainer;

public interface KafkaTestContainer {

    ConfluentKafkaContainer KAFKA_CONTAINER = createAndStartContainer();

    private static ConfluentKafkaContainer createAndStartContainer() {
        ConfluentKafkaContainer container = new ConfluentKafkaContainer(
            "confluentinc/cp-kafka:7.8.0")
            .withReuse(true);
        container.start();
        return container;
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
            KAFKA_CONTAINER::getBootstrapServers);
    }
}
