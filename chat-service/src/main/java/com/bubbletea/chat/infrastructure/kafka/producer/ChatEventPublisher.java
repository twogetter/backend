package com.bubbletea.chat.infrastructure.kafka.producer;

import com.bubbletea.chat.domain.event.ChatPublishedEvent;
import com.bubbletea.chat.infrastructure.kafka.config.ChatKafkaTopics;
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
public class ChatEventPublisher {

  private static final String HEADER_DOMAIN = "domainName";
  private static final String DOMAIN_NAME = "chat";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  public void publish(ChatPublishedEvent event) {

    Message<ChatPublishedEvent> message = MessageBuilder
        .withPayload(event)
        .setHeader(KafkaHeaders.TOPIC, ChatKafkaTopics.CHAT_PUBLISHED)
        .setHeader(HEADER_DOMAIN, DOMAIN_NAME)
        .build();

    kafkaTemplate.send(message).whenComplete((result, ex) -> {
      if (ex != null) {
        log.error("[Kafka 송신 실패] topic={}, event={}", ChatKafkaTopics.CHAT_PUBLISHED, event,
            ex);
        return;
      }

      log.info("[Kafka 송신] 채팅 이벤트: topic={}, partition={}, offset={}",
          ChatKafkaTopics.CHAT_PUBLISHED, result.getRecordMetadata().partition(),
          result.getRecordMetadata().offset());
    });
  }
}
