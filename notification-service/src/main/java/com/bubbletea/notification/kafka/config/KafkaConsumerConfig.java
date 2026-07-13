package com.bubbletea.notification.kafka.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

  private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
  private static final String DEFAULT_GROUP_ID = "notification-service";
  private static final String DEFAULT_AUTO_OFFSET_RESET = "earliest";
  private static final int CONCURRENCY = 1;
  private static final long RETRY_INTERVAL_MILLISECONDS = 1_000L;
  private static final long RETRY_MAX_ATTEMPTS = 2L;

  @Bean
  public ConsumerFactory<String, String> consumerFactory(Environment environment) {
    Map<String, Object> properties = new HashMap<>();

    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers(environment));
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, getGroupId(environment));
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, getAutoOffsetReset(environment));
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    return new DefaultKafkaConsumerFactory<>(properties);
  }

  @Bean
  public ProducerFactory<String, String> producerFactory(Environment environment) {
    Map<String, Object> properties = new HashMap<>();

    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers(environment));
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    properties.put(ProducerConfig.ACKS_CONFIG, "all");

    return new DefaultKafkaProducerFactory<>(properties);
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(
      ProducerFactory<String, String> producerFactory
  ) {
    return new KafkaTemplate<>(producerFactory);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ConsumerFactory<String, String> consumerFactory,
      CommonErrorHandler commonErrorHandler
  ) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory =
        new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(consumerFactory);
    factory.setConcurrency(CONCURRENCY);
    factory.setCommonErrorHandler(commonErrorHandler);
    factory.setMissingTopicsFatal(false);
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

    return factory;
  }

  @Bean
  public CommonErrorHandler commonErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        kafkaTemplate,
        (record, exception) -> new TopicPartition(
            record.topic() + KafkaTopicConfig.DEAD_LETTER_TOPIC_SUFFIX,
            record.partition()
        )
    );
    recoverer.setFailIfSendResultIsError(true);

    return new DefaultErrorHandler(recoverer, new FixedBackOff(
        RETRY_INTERVAL_MILLISECONDS,
        RETRY_MAX_ATTEMPTS
    ));
  }

  private String getBootstrapServers(Environment environment) {
    return environment.getProperty("spring.kafka.bootstrap-servers", DEFAULT_BOOTSTRAP_SERVERS);
  }

  private String getGroupId(Environment environment) {
    return environment.getProperty(
        "spring.kafka.consumer.group-id",
        environment.getProperty("NOTIFICATION_KAFKA_GROUP_ID", DEFAULT_GROUP_ID)
    );
  }

  private String getAutoOffsetReset(Environment environment) {
    return environment.getProperty("spring.kafka.consumer.auto-offset-reset", DEFAULT_AUTO_OFFSET_RESET);
  }
}
