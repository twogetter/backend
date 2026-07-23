package com.bubbletea.chat.infrastructure.kafka.consumer.order;

import com.bubbletea.chat.application.service.ChatParticipantService;
import com.bubbletea.chat.application.service.ChatRoomService;
import com.bubbletea.chat.domain.entity.ChatRoom;
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
  private final ChatRoomService chatRoomService;
  private final ChatParticipantService chatParticipantService;

  @KafkaListener(topics = ChatKafkaTopics.ORDER_CREATED, groupId = "chat-service")
  public void consume(String message) {
    try {
      OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
      log.info("[Kafka 수신] 구독 생성 이벤트 : {}", event);

      ChatRoom chatRoom = chatRoomService.getChatRoomByArtistId(event.artistId());
      Long participantId = chatParticipantService.save(chatRoom.getId(), event.fanId(), event.startedAt());
      log.info("[팬 입장 완료] chatRoomId: {}, fanId: {}, participantId: {}", chatRoom.getId(),
          event.fanId(), participantId);
    } catch (Exception e) {
      log.error("[Kafka 수신 실패] : {}", message, e);
      throw new RuntimeException(e);
    }
  }
}
