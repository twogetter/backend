package com.bubbletea.order.infrastructure.config;

import com.bubbletea.order.domain.event.PaymentResultEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

/**
 * payment-service의 결제 결과({@code PaymentResultEvent}) 소비 설정.
 * payment 프로듀서가 {@code JacksonJsonSerializer}(타입 헤더 미포함)로 발행하므로,
 * 역직렬화 대상 타입을 명시해 헤더 없이도 매핑되게 한다.
 */
@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers:bubbletea-kafka:9094}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id:order-service}")
  private String groupId;

  @Bean
  public ConsumerFactory<String, PaymentResultEvent> paymentResultConsumerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    JacksonJsonDeserializer<PaymentResultEvent> valueDeserializer =
        new JacksonJsonDeserializer<>(PaymentResultEvent.class);
    valueDeserializer.addTrustedPackages("com.bubbletea.*");

    return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, PaymentResultEvent>
      paymentResultListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, PaymentResultEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(paymentResultConsumerFactory());
    return factory;
  }
}
