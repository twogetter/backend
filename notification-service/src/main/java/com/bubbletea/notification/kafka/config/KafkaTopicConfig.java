package com.bubbletea.notification.kafka.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfig {

  public static final String DEAD_LETTER_TOPIC_SUFFIX = "-dlt";
  public static final String PRODUCT_ACTIVATION_SCHEDULED_TOPIC = "notification.product.productActivationScheduled";
  public static final String PRODUCT_DELETION_SCHEDULED_TOPIC = "notification.product.productDeletionScheduled";
  public static final String PRODUCT_PRICE_CHANGE_SCHEDULED_TOPIC = "notification.product.productPriceChangeScheduled";
  public static final String PRODUCT_DEACTIVATION_SCHEDULED_TOPIC = "notification.product.productDeactivationScheduled";
  public static final String PRODUCT_OPEN_SCHEDULED_TOPIC = "notification.product.productOpenScheduled";
  public static final String PAYMENT_COMPLETE_TOPIC = "notification.payment.paymentComplete";
  public static final String PAYMENT_FAIL_TOPIC = "notification.payment.paymentFail";
  public static final String MEMBER_SIGNED_UP_TOPIC = "notification.member.signed-up";
  public static final String MEMBER_AUTH_LOGIN_TOPIC = "notification.member.auth-login";
  public static final String MEMBER_PASSWORD_CHANGED_TOPIC = "notification.member.password-changed";
  public static final String MEMBER_DELETE_ACCOUNT_TOPIC = "notification.member.delete-account";
  public static final String CHAT_PUBLISHED_TOPIC = "notification.chat.published";
  public static final String SUBSCRIBE_RENEWAL_TOPIC = "notification.order.subscribeRenewal";

  private static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
  private static final int DEFAULT_PARTITIONS = 1;
  private static final int DEFAULT_REPLICAS = 1;

  @Bean
  public KafkaAdmin kafkaAdmin(Environment environment) {
    Map<String, Object> configs = new HashMap<>();
    configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers(environment));

    KafkaAdmin kafkaAdmin = new KafkaAdmin(configs);
    kafkaAdmin.setFatalIfBrokerNotAvailable(false);

    return kafkaAdmin;
  }

  // 로컬 테스트를 위한 토픽 생성
  // msa 통합 테스트 진행 시 알림이 아닌 producer 도메인에서 생성할 필요가 있음
  @Bean
  public NewTopic productActivationScheduledTopic() {
    return createTopic(PRODUCT_ACTIVATION_SCHEDULED_TOPIC);
  }

  @Bean
  public NewTopic productDeletionScheduledTopic() {
    return createTopic(PRODUCT_DELETION_SCHEDULED_TOPIC);
  }

  @Bean
  public NewTopic productPriceChangeScheduledTopic() {
    return createTopic(PRODUCT_PRICE_CHANGE_SCHEDULED_TOPIC);
  }

  @Bean
  public NewTopic productDeactivationScheduledTopic() {
    return createTopic(PRODUCT_DEACTIVATION_SCHEDULED_TOPIC);
  }

  @Bean
  public NewTopic productOpenScheduledTopic() {
    return createTopic(PRODUCT_OPEN_SCHEDULED_TOPIC);
  }

  @Bean
  public NewTopic paymentCompleteTopic() {
    return createTopic(PAYMENT_COMPLETE_TOPIC);
  }

  @Bean
  public NewTopic paymentFailTopic() {
    return createTopic(PAYMENT_FAIL_TOPIC);
  }

  @Bean
  public NewTopic memberSignedUpTopic() {
    return createTopic(MEMBER_SIGNED_UP_TOPIC);
  }

  @Bean
  public NewTopic memberAuthLoginTopic() {
    return createTopic(MEMBER_AUTH_LOGIN_TOPIC);
  }

  @Bean
  public NewTopic memberPasswordChangedTopic() {
    return createTopic(MEMBER_PASSWORD_CHANGED_TOPIC);
  }

  @Bean
  public NewTopic memberDeleteAccountTopic() {
    return createTopic(MEMBER_DELETE_ACCOUNT_TOPIC);
  }

  @Bean
  public NewTopic chatPublishedTopic() {
    return createTopic(CHAT_PUBLISHED_TOPIC);
  }

  @Bean
  public NewTopic subscribeRenewalTopic() {
    return createTopic(SUBSCRIBE_RENEWAL_TOPIC);
  }

  @Bean
  public KafkaAdmin.NewTopics deadLetterTopics() {
    return new KafkaAdmin.NewTopics(
        createDeadLetterTopic(PRODUCT_ACTIVATION_SCHEDULED_TOPIC),
        createDeadLetterTopic(PRODUCT_DELETION_SCHEDULED_TOPIC),
        createDeadLetterTopic(PRODUCT_PRICE_CHANGE_SCHEDULED_TOPIC),
        createDeadLetterTopic(PRODUCT_DEACTIVATION_SCHEDULED_TOPIC),
        createDeadLetterTopic(PRODUCT_OPEN_SCHEDULED_TOPIC),
        createDeadLetterTopic(PAYMENT_COMPLETE_TOPIC),
        createDeadLetterTopic(PAYMENT_FAIL_TOPIC),
        createDeadLetterTopic(MEMBER_SIGNED_UP_TOPIC),
        createDeadLetterTopic(MEMBER_AUTH_LOGIN_TOPIC),
        createDeadLetterTopic(MEMBER_PASSWORD_CHANGED_TOPIC),
        createDeadLetterTopic(MEMBER_DELETE_ACCOUNT_TOPIC),
        createDeadLetterTopic(CHAT_PUBLISHED_TOPIC),
        createDeadLetterTopic(SUBSCRIBE_RENEWAL_TOPIC)
    );
  }

  private NewTopic createTopic(String topicName) {
    return TopicBuilder.name(topicName)
        .partitions(DEFAULT_PARTITIONS)
        .replicas(DEFAULT_REPLICAS)
        .build();
  }

  private NewTopic createDeadLetterTopic(String topicName) {
    return createTopic(topicName + DEAD_LETTER_TOPIC_SUFFIX);
  }

  private String getBootstrapServers(Environment environment) {
    return environment.getProperty("spring.kafka.bootstrap-servers", DEFAULT_BOOTSTRAP_SERVERS);
  }
}
