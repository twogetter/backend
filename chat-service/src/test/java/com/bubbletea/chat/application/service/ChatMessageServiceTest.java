package com.bubbletea.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.application.dto.ChatMessageResponseDto;
import com.bubbletea.chat.application.service.validator.ActiveParticipantValidator;
import com.bubbletea.chat.application.service.validator.FanMessageValidator;
import com.bubbletea.chat.domain.entity.ChatMessage;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.MessageType;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatMessageRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.chat.presentation.controller.dto.ChatMessageCreateRequestDto;
import com.bubbletea.common.exception.AppException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @Mock
  private ActiveParticipantValidator activeParticipantValidator;

  @Mock
  private FanMessageValidator fanMessageValidator;

  @InjectMocks
  private ChatMessageService chatMessageService;

  @Test
  @DisplayName("팬이 메시지를 보내면 검증 후 저장된다")
  void save_Fan_Success() {
    // given
    Long roomId = 1L;
    Long senderId = 2L;
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("ㅎㅇ",
        MessageType.TEXT);
    ChatRoom mockRoom = ChatRoom.create(10L);

    ChatMessage mockMessage = ChatMessage.builder()
        .roomId(roomId)
        .senderId(senderId)
        .senderType(ParticipantRole.FAN)
        .content("ㅎㅇ")
        .messageType(MessageType.TEXT)
        .build();
    org.springframework.test.util.ReflectionTestUtils.setField(mockMessage, "id", 100L);

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
    doNothing().when(activeParticipantValidator).validate(roomId, senderId);
    doNothing().when(fanMessageValidator).validate(roomId, senderId);
    when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);

    // when
    ChatMessageResponseDto response = chatMessageService.save(roomId, senderId, ParticipantRole.FAN,
        requestDto);

    // then
    assertThat(response.id()).isEqualTo(100L);
    assertThat(response.content()).isEqualTo("ㅎㅇ");
    assertThat(response.senderType()).isEqualTo(ParticipantRole.FAN);
    verify(chatRoomRepository, times(1)).findById(roomId);
    verify(activeParticipantValidator, times(1)).validate(roomId, senderId);
    verify(fanMessageValidator, times(1)).validate(roomId, senderId);
    verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
  }

  @Test
  @DisplayName("아티스트가 메시지를 보낸 후 저장된다")
  void save_Artist_Success() {
    // given
    Long roomId = 1L;
    Long senderId = 10L;
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("여러분",
        MessageType.TEXT);
    ChatRoom mockRoom = ChatRoom.create(senderId);

    ChatMessage mockMessage = ChatMessage.builder()
        .roomId(roomId)
        .senderId(senderId)
        .senderType(ParticipantRole.ARTIST)
        .content("여러분")
        .messageType(MessageType.TEXT)
        .build();
    org.springframework.test.util.ReflectionTestUtils.setField(mockMessage, "id", 101L);

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
    doNothing().when(activeParticipantValidator).validate(roomId, senderId);
    when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);

    // when
    ChatMessageResponseDto response = chatMessageService.save(roomId, senderId,
        ParticipantRole.ARTIST, requestDto);

    // then
    assertThat(response.id()).isEqualTo(101L);
    assertThat(response.content()).isEqualTo("여러분");
    assertThat(response.senderType()).isEqualTo(ParticipantRole.ARTIST);
    verify(chatRoomRepository, times(1)).findById(roomId);
    verify(activeParticipantValidator, times(1)).validate(roomId, senderId);
    verify(fanMessageValidator, times(0)).validate(any(), any());
    verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
  }

  @Test
  @DisplayName("존재하지 않는 채팅방에 전송 시 CHAT_ROOM_NOT_FOUND 에러를 던진다")
  void save_RoomNotFound() {
    // given
    Long roomId = 999L;
    Long senderId = 2L;
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("ㅎㅇ",
        MessageType.TEXT);

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(
        () -> chatMessageService.save(roomId, senderId, ParticipantRole.FAN, requestDto))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_ROOM_NOT_FOUND);
  }

  @Test
  @DisplayName("삭제된 채팅방에 메시지 전송 시 CHAT_ROOM_NOT_FOUND 에러를 던진다")
  void save_RoomDeleted() {
    // given
    Long roomId = 1L;
    Long senderId = 2L;
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("ㅎㅇ",
        MessageType.TEXT);
    ChatRoom mockRoom = ChatRoom.create(10L);
    mockRoom.delete(); // Status becomes DELETED

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

    // when & then
    assertThatThrownBy(
        () -> chatMessageService.save(roomId, senderId, ParticipantRole.FAN, requestDto))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_ROOM_NOT_FOUND);
  }

  @Test
  @DisplayName("참여자가 아니거나 비활성화된 참여자가 전송 시 예외가 발생한다")
  void save_InactiveParticipant() {
    // given
    Long roomId = 1L;
    Long senderId = 2L;
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("ㅎㅇ",
        MessageType.TEXT);
    ChatRoom mockRoom = ChatRoom.create(10L);

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
    doThrow(new AppException(ChatErrorCode.PARTICIPANT_NOT_FOUND))
        .when(activeParticipantValidator).validate(roomId, senderId);

    // when & then
    assertThatThrownBy(
        () -> chatMessageService.save(roomId, senderId, ParticipantRole.FAN, requestDto))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.PARTICIPANT_NOT_FOUND);
  }

  @Test
  @DisplayName("팬이 전송 한도를 초과하면 예외가 발생한다")
  void save_FanExceededLimit() {
    // given
    Long roomId = 1L;
    Long senderId = 2L;
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("ㅎㅇ",
        MessageType.TEXT);
    ChatRoom mockRoom = ChatRoom.create(10L);

    when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
    doNothing().when(activeParticipantValidator).validate(roomId, senderId);
    doThrow(new AppException(ChatErrorCode.EXCEEDED_DAILY_LIMIT))
        .when(fanMessageValidator).validate(roomId, senderId);

    // when & then
    assertThatThrownBy(
        () -> chatMessageService.save(roomId, senderId, ParticipantRole.FAN, requestDto))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.EXCEEDED_DAILY_LIMIT);
  }
}
