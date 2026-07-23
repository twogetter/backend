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
public class OrderExpiredEventConsumer {

  private final ObjectMapper objectMapper;
  private final ChatRoomService chatRoomService;
  private final ChatParticipantService chatParticipantService;

  @KafkaListener(topics = ChatKafkaTopics.ORDER_EXPIRED, groupId = "chat-service")
  public void consume(String message) {
    try {
      OrderExpiredEvent event = objectMapper.readValue(message, OrderExpiredEvent.class);
      log.info("[Kafka 수신] 구독 종료 이벤트 : {}", event);

      ChatRoom chatRoom = chatRoomService.getChatRoomByArtistId(event.artistId());
      chatParticipantService.delete(chatRoom.getId(), event.fanId(), event.endedAt());
      log.info("[팬 퇴장 완료] chatRoomId: {}, fanId: {}", chatRoom.getId(), event.fanId());
    } catch (Exception e) {
      log.error("[Kafka 수신 실패] : {}", message, e);
      throw new RuntimeException(e);
    }
  }
}
