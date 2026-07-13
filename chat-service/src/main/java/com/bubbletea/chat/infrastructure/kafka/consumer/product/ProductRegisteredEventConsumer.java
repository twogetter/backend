package com.bubbletea.chat.infrastructure.kafka.consumer.product;

import com.bubbletea.chat.application.service.ChatRoomService;
import com.bubbletea.chat.infrastructure.kafka.config.ChatKafkaTopics;
import com.bubbletea.common.exception.AppException;
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
  private final ChatRoomService chatRoomService;

  @KafkaListener(topics = ChatKafkaTopics.PRODUCT_REGISTERED, groupId = "chat-service")
  public void consume(String message) {
    try {
      ProductRegisteredEvent event = objectMapper.readValue(message, ProductRegisteredEvent.class);
      log.info("[Kafka 수신] 상품 등록 이벤트 : {}", event);
      try {
        Long chatRoomId = chatRoomService.createChatRoom(event.artistId());
        log.info("[채팅방 생성 완료] artistId: {}, chatRoomId: {}", event.artistId(), chatRoomId);

      } catch (AppException e) {
        log.warn("[채팅방 생성 실패] 비즈니스 예외 발생 - 코드: {}, 메시지: {}", e.getErrorCode().getCode(),
            e.getMessage());
      }
    } catch (Exception e) {
      log.error("[Kafka 수신 실패] : {}", message, e);
      throw new RuntimeException(e);
    }
  }
}
