package com.bubbletea.chat.infrastructure.kafka.consumer.order;

import com.bubbletea.chat.infrastructure.kafka.config.ChatKafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

  private final ObjectMapper objectMapper;

  @KafkaListener(topics = ChatKafkaTopics.ORDER_CREATED, groupId = "chat-service")
  public void consume(String message) {
    try {
      OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
      log.info("[Kafka 수신] 구독 생성 이벤트 : {}", event);
      // TODO: 추후 비즈니스 서비스 연동
    } catch (Exception e) {
      log.error("[Kafka 수신 실패] : {}", message, e);
      throw new RuntimeException(e);
    }
  }
}
