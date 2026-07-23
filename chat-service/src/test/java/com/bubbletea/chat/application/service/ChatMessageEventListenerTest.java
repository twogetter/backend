package com.bubbletea.chat.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.application.dto.ChatMessageResponseDto;
import com.bubbletea.chat.domain.entity.ChatMessage;
import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.enums.MessageType;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.event.ChatMessageSavedEvent;
import com.bubbletea.chat.domain.event.ChatPublishedEvent;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.infrastructure.kafka.producer.ChatEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatMessageEventListenerTest {

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private ChatParticipantRepository chatParticipantRepository;

  @Mock
  private ChatEventPublisher chatEventPublisher;

  @InjectMocks
  private ChatMessageEventListener chatMessageEventListener;

  @Test
  @DisplayName("아티스트 메시지 저장 이벤트 수신 시 팬 브로드캐스트 및 Kafka 이벤트가 발행된다")
  void handleMessageSaved_Artist_Success() {
    // given
    Long roomId = 1L;
    ChatMessage mockMessage = ChatMessage.builder()
        .roomId(roomId)
        .senderId(10L)
        .senderType(ParticipantRole.ARTIST)
        .content("여러분")
        .messageType(MessageType.TEXT)
        .build();
    org.springframework.test.util.ReflectionTestUtils.setField(mockMessage, "id", 100L);
    org.springframework.test.util.ReflectionTestUtils.setField(mockMessage, "createdAt",
        LocalDateTime.now());

    ChatMessageSavedEvent savedEvent = new ChatMessageSavedEvent(mockMessage,
        ParticipantRole.ARTIST, "아이돌");
    ChatParticipant fanParticipant = ChatParticipant.createFanParticipant(roomId, 20L);

    when(chatParticipantRepository.findAllByRoomIdAndStatus(roomId, ParticipantStatus.ACTIVE))
        .thenReturn(List.of(fanParticipant));

    // when
    chatMessageEventListener.handleMessageSaved(savedEvent);

    // then
    verify(messagingTemplate, times(1)).convertAndSend(eq("/sub/rooms/1/artist"),
        any(ChatMessageResponseDto.class));
    verify(chatEventPublisher, times(1)).publish(any(ChatPublishedEvent.class));
  }

  @Test
  @DisplayName("팬 메시지 저장 이벤트 수신 시 아티스트 대역으로 전송된다")
  void handleMessageSaved_Fan_Success() {
    // given
    Long roomId = 1L;
    ChatMessage mockMessage = ChatMessage.builder()
        .roomId(roomId)
        .senderId(20L)
        .senderType(ParticipantRole.FAN)
        .content("팬 메시지")
        .messageType(MessageType.TEXT)
        .build();
    org.springframework.test.util.ReflectionTestUtils.setField(mockMessage, "id", 101L);
    org.springframework.test.util.ReflectionTestUtils.setField(mockMessage, "createdAt",
        LocalDateTime.now());

    ChatMessageSavedEvent savedEvent = new ChatMessageSavedEvent(mockMessage, ParticipantRole.FAN,
        "팬");

    // when
    chatMessageEventListener.handleMessageSaved(savedEvent);

    // then
    verify(messagingTemplate, times(1)).convertAndSend(eq("/sub/rooms/1/fan"),
        any(ChatMessageResponseDto.class));
  }
}
