package com.bubbletea.chat.infrastructure.kafka.consumer.product;

import com.bubbletea.chat.infrastructure.kafka.config.ChatKafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRegisteredEventConsumer {

  private final ObjectMapper objectMapper;

  @KafkaListener(topics = ChatKafkaTopics.PRODUCT_REGISTERED, groupId = "chat-service")
  public void consume(String message) {
    try {
      ProductRegisteredEvent event = objectMapper.readValue(message, ProductRegisteredEvent.class);
      log.info("[Kafka 수신] 상품 등록 이벤트 : {}", event);
      // TODO: application Layer 연결
    } catch (Exception e) {
      log.error("[Kafka 수신 실패] : {}", message, e);
    }
  }
}
