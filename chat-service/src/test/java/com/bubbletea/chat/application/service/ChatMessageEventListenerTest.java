package com.bubbletea.chat.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.MessageType;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.chat.infrastructure.kafka.producer.ChatEventPublisher;
import com.bubbletea.chat.presentation.controller.dto.ChatMessageCreateRequestDto;
import com.bubbletea.chat.support.ChatIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class ChatMessageEventListenerTest extends ChatIntegrationTestSupport {

  @Autowired
  private ChatMessageService chatMessageService;

  @Autowired
  private ChatRoomRepository chatRoomRepository;

  @Autowired
  private ChatParticipantRepository chatParticipantRepository;

  @MockitoBean
  private SimpMessagingTemplate messagingTemplate;

  @MockitoBean
  private ChatEventPublisher chatEventPublisher;

  private ChatRoom chatRoom;

  @BeforeEach
  void setUp() {
    chatRoom = chatRoomRepository.save(ChatRoom.create(1L));

    chatParticipantRepository.save(ChatParticipant.builder()
        .roomId(chatRoom.getId())
        .userId(1L)
        .role(ParticipantRole.ARTIST)
        .status(ParticipantStatus.ACTIVE)
        .lastReadId(0L)
        .build());

    chatParticipantRepository.save(ChatParticipant.builder()
        .roomId(chatRoom.getId())
        .userId(2L)
        .role(ParticipantRole.FAN)
        .status(ParticipantStatus.ACTIVE)
        .lastReadId(0L)
        .build());
  }

  @AfterEach
  void tearDown() {
    chatParticipantRepository.deleteAllInBatch();
    chatRoomRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("아티스트가 메시지를 전송하면 웹소켓 아티스트 채널과 카프카로 팬들에게 이벤트가 발행된다")
  void handleMessageSaved_ByArtist() {
    // given
    Long artistUserId = 1L;
    ParticipantRole role = ParticipantRole.ARTIST;
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto(
        "여러분",
        MessageType.TEXT
    );

    // when
    chatMessageService.save(chatRoom.getId(), artistUserId, role, requestDto);

    // then
    verify(messagingTemplate).convertAndSend(
        eq("/sub/rooms/" + chatRoom.getId() + "/artist"),
        any(Object.class)
    );

    verify(chatEventPublisher).publish(argThat(event ->
        event.memberId().equals(2L) &&
            event.message().equals("여러분")
    ));
  }

  @Test
  @DisplayName("팬이 메시지를 전송하면 웹소켓 팬 채널로만 브로드캐스트되고 카프카 이벤트는 발행되지 않는다")
  void handleMessageSaved_ByFan() {
    // given
    Long fanUserId = 2L;
    ParticipantRole role = ParticipantRole.FAN;
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto(
        "팬 메시지",
        MessageType.TEXT
    );

    // when
    chatMessageService.save(chatRoom.getId(), fanUserId, role, requestDto);

    // then
    verify(messagingTemplate).convertAndSend(
        eq("/sub/rooms/" + chatRoom.getId() + "/fan"),
        any(Object.class)
    );

    verify(messagingTemplate, never()).convertAndSend(
        eq("/sub/rooms/" + chatRoom.getId() + "/artist"),
        any(Object.class)
    );
    
    verify(chatEventPublisher, never()).publish(any());
  }
}
