package com.bubbletea.payment.infrastructure.config;

import com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:bubbletea-kafka:9094}")
    private String bootstrapServers;

    @Bean
    public DefaultKafkaConsumerFactory<String, BillingEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // 현재 사용 중이신 정확한 카프카 주소 지정
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "billing-payment-group");

        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        JacksonJsonDeserializer<BillingEvent> jacksonDeserializer = new JacksonJsonDeserializer<>(BillingEvent.class);
        jacksonDeserializer.addTrustedPackages("*");
        // UI에서 String으로 보내므로 둘 다 StringDeserializer로 설정
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);


//        return new DefaultKafkaConsumerFactory<>(props);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(), // Key 역직렬화
                jacksonDeserializer        // Value 역직렬화 (JacksonJsonDeserializer 적용)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BillingEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, BillingEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        // ack.acknowledge()가 정상 동작하기 위해 반드시 필요한 설정!
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}