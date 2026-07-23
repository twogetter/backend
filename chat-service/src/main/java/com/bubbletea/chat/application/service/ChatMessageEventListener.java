package com.bubbletea.chat.application.service;

import com.bubbletea.chat.application.dto.ChatMessageResponseDto;
import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.event.ChatMessageSavedEvent;
import com.bubbletea.chat.domain.event.ChatPublishedEvent;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.infrastructure.kafka.producer.ChatEventPublisher;
import com.bubbletea.common.utility.KafkaEventIdConverter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventListener {

  private final SimpMessagingTemplate messagingTemplate;
  private final ChatParticipantRepository chatParticipantRepository;
  private final ChatEventPublisher chatEventPublisher;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleMessageSaved(ChatMessageSavedEvent event) {
    Long roomId = event.savedMessage().getRoomId();
    ParticipantRole role = event.role();
    ChatMessageResponseDto responseDto = ChatMessageResponseDto.from(event.savedMessage());

    if (role == ParticipantRole.ARTIST) {
      // 웹소켓 팬 대역 브로드캐스트
      messagingTemplate.convertAndSend("/sub/rooms/" + roomId + "/artist", responseDto);

      List<ChatParticipant> participants = chatParticipantRepository.findAllByRoomIdAndStatus(
          roomId, ParticipantStatus.ACTIVE);
      String artistName = (event.nickname() != null) ? event.nickname() : "아티스트";

      for (ChatParticipant cp : participants) {
        if (cp.getRole() == ParticipantRole.FAN) {
          String eventId = KafkaEventIdConverter.convert(
              "chat",
              "CHAT_PUBLISHED",
              cp.getUserId(),
              event.savedMessage().getCreatedAt()
          );

          chatEventPublisher.publish(ChatPublishedEvent.of(
              eventId,
              cp.getUserId(),
              artistName,
              event.savedMessage().getContent(),
              event.savedMessage().getMessageType(),
              event.savedMessage().getCreatedAt()
          ));
        }
      }
    } else if (role == ParticipantRole.FAN) {
      // 웹소켓 아티스트 대역 격리 송신
      messagingTemplate.convertAndSend("/sub/rooms/" + roomId + "/fan", responseDto);
    }
  }
}
