package com.bubbletea.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.event.ChatPublishedEvent;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.infrastructure.kafka.producer.ChatEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @Mock
  private ChatParticipantRepository chatParticipantRepository;

  @Mock
  private ActiveParticipantValidator activeParticipantValidator;

  @Mock
  private FanMessageValidator fanMessageValidator;

  @Mock
  private SimpMessagingTemplate messagingTemplate;

  @Mock
  private ChatEventPublisher chatEventPublisher;

  @InjectMocks
  private ChatMessageService chatMessageService;

  @Nested
  @DisplayName("메시지 전송")
  class SaveMessage {

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
      doNothing().when(activeParticipantValidator).validate(roomId, senderId, ParticipantRole.FAN);
      doNothing().when(fanMessageValidator).validate(roomId, senderId, ParticipantRole.FAN);
      when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);

      // when
      ChatMessageResponseDto response = chatMessageService.save(roomId, senderId,
          ParticipantRole.FAN,
          requestDto);

      // then
      assertThat(response.id()).isEqualTo(100L);
      assertThat(response.content()).isEqualTo("ㅎㅇ");
      assertThat(response.senderType()).isEqualTo(ParticipantRole.FAN);
      verify(chatRoomRepository, times(1)).findById(roomId);
      verify(activeParticipantValidator, times(1)).validate(roomId, senderId, ParticipantRole.FAN);
      verify(fanMessageValidator, times(1)).validate(roomId, senderId, ParticipantRole.FAN);
      verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
      verify(messagingTemplate, times(1)).convertAndSend(eq("/sub/rooms/1/fan"), any(ChatMessageResponseDto.class));
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
      org.springframework.test.util.ReflectionTestUtils.setField(mockMessage, "createdAt", java.time.LocalDateTime.now());

      when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
      doNothing().when(activeParticipantValidator)
          .validate(roomId, senderId, ParticipantRole.ARTIST);
      when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(mockMessage);

      ChatParticipant mockFanParticipant = ChatParticipant.createFanParticipant(roomId, 20L);
      when(chatParticipantRepository.findAllByRoomIdAndStatus(roomId, ParticipantStatus.ACTIVE))
          .thenReturn(List.of(mockFanParticipant));

      // when
      ChatMessageResponseDto response = chatMessageService.save(roomId, senderId,
          ParticipantRole.ARTIST, requestDto);

      // then
      assertThat(response.id()).isEqualTo(101L);
      assertThat(response.content()).isEqualTo("여러분");
      assertThat(response.senderType()).isEqualTo(ParticipantRole.ARTIST);
      verify(chatRoomRepository, times(1)).findById(roomId);
      verify(activeParticipantValidator, times(1)).validate(roomId, senderId,
          ParticipantRole.ARTIST);
      verify(fanMessageValidator, times(0)).validate(any(), any(), any());
      verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
      verify(messagingTemplate, times(1)).convertAndSend(eq("/sub/rooms/1/artist"), any(ChatMessageResponseDto.class));
      verify(chatEventPublisher, times(1)).publish(any(ChatPublishedEvent.class));
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
          .when(activeParticipantValidator).validate(roomId, senderId, ParticipantRole.FAN);

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
      doNothing().when(activeParticipantValidator).validate(roomId, senderId, ParticipantRole.FAN);
      doThrow(new AppException(ChatErrorCode.EXCEEDED_DAILY_LIMIT))
          .when(fanMessageValidator).validate(roomId, senderId, ParticipantRole.FAN);

      // when & then
      assertThatThrownBy(
          () -> chatMessageService.save(roomId, senderId, ParticipantRole.FAN, requestDto))
          .isInstanceOf(AppException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.EXCEEDED_DAILY_LIMIT);
    }
  }

  @Nested
  @DisplayName("메시지 내역 조회")
  class GetMessages {

    @Test
    @DisplayName("아티스트 권한으로 커서 없이 전체 메시지 목록을 가져온다")
    void getAll_Artist_WithoutCursor_Success() {
      // given
      Long roomId = 1L;
      Long requesterId = 10L;
      ChatRoom mockRoom = ChatRoom.create(requesterId);
      ChatMessage msg = ChatMessage.builder()
          .roomId(roomId)
          .senderId(requesterId)
          .senderType(ParticipantRole.ARTIST)
          .content("ㅎㅇ")
          .messageType(MessageType.TEXT)
          .build();
      org.springframework.test.util.ReflectionTestUtils.setField(msg, "id", 100L);

      when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
      doNothing().when(activeParticipantValidator)
          .validate(roomId, requesterId, ParticipantRole.ARTIST);
      when(chatMessageRepository.findByRoomIdOrderByIdDesc(roomId, PageRequest.of(0, 20)))
          .thenReturn(List.of(msg));

      // when
      List<ChatMessageResponseDto> result = chatMessageService.getAll(roomId, requesterId,
          ParticipantRole.ARTIST, null, 20);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().id()).isEqualTo(100L);
      assertThat(result.getFirst().content()).isEqualTo("ㅎㅇ");
      verify(chatRoomRepository).findById(roomId);
      verify(activeParticipantValidator).validate(roomId, requesterId, ParticipantRole.ARTIST);
      verify(chatMessageRepository).findByRoomIdOrderByIdDesc(roomId, PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("아티스트 권한으로 커서 미만 전체 메시지 목록을 가져온다")
    void getAll_Artist_WithCursor_Success() {
      // given
      Long roomId = 1L;
      Long requesterId = 10L;
      Long cursorId = 150L;
      ChatRoom mockRoom = ChatRoom.create(requesterId);
      ChatMessage msg = ChatMessage.builder()
          .roomId(roomId)
          .senderId(requesterId)
          .senderType(ParticipantRole.ARTIST)
          .content("ㅎㅇ")
          .messageType(MessageType.TEXT)
          .build();
      org.springframework.test.util.ReflectionTestUtils.setField(msg, "id", 140L);

      when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
      doNothing().when(activeParticipantValidator)
          .validate(roomId, requesterId, ParticipantRole.ARTIST);
      when(chatMessageRepository.findByRoomIdAndIdLessThanOrderByIdDesc(roomId, cursorId,
          PageRequest.of(0, 20)))
          .thenReturn(List.of(msg));

      // when
      List<ChatMessageResponseDto> result = chatMessageService.getAll(roomId, requesterId,
          ParticipantRole.ARTIST, cursorId, 20);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().id()).isEqualTo(140L);
      verify(chatMessageRepository).findByRoomIdAndIdLessThanOrderByIdDesc(roomId, cursorId,
          PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("팬 권한으로 커서 없이 본인 및 아티스트 메시지 목록을 가져온다")
    void getAll_Fan_WithoutCursor_Success() {
      // given
      Long roomId = 1L;
      Long requesterId = 2L;
      ChatRoom mockRoom = ChatRoom.create(10L);
      ChatMessage msg = ChatMessage.builder()
          .roomId(roomId)
          .senderId(requesterId)
          .senderType(ParticipantRole.FAN)
          .content("우와")
          .messageType(MessageType.TEXT)
          .build();
      org.springframework.test.util.ReflectionTestUtils.setField(msg, "id", 100L);

      when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
      doNothing().when(activeParticipantValidator)
          .validate(roomId, requesterId, ParticipantRole.FAN);
      when(chatMessageRepository.findFanMessages(roomId, requesterId, PageRequest.of(0, 20)))
          .thenReturn(List.of(msg));

      // when
      List<ChatMessageResponseDto> result = chatMessageService.getAll(roomId, requesterId,
          ParticipantRole.FAN, null, 20);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().id()).isEqualTo(100L);
      verify(chatMessageRepository).findFanMessages(roomId, requesterId, PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("팬 권한으로 커서 미만 본인 및 아티스트 메시지 목록을 가져온다")
    void getAll_Fan_WithCursor_Success() {
      // given
      Long roomId = 1L;
      Long requesterId = 2L;
      Long cursorId = 120L;
      ChatRoom mockRoom = ChatRoom.create(10L);
      ChatMessage msg = ChatMessage.builder()
          .roomId(roomId)
          .senderId(10L)
          .senderType(ParticipantRole.ARTIST)
          .content("우와")
          .messageType(MessageType.TEXT)
          .build();
      org.springframework.test.util.ReflectionTestUtils.setField(msg, "id", 110L);

      when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
      doNothing().when(activeParticipantValidator)
          .validate(roomId, requesterId, ParticipantRole.FAN);
      when(chatMessageRepository.findFanMessagesWithCursor(roomId, requesterId, cursorId,
          PageRequest.of(0, 20)))
          .thenReturn(List.of(msg));

      // when
      List<ChatMessageResponseDto> result = chatMessageService.getAll(roomId, requesterId,
          ParticipantRole.FAN, cursorId, 20);

      // then
      assertThat(result).hasSize(1);
      assertThat(result.getFirst().id()).isEqualTo(110L);
      verify(chatMessageRepository).findFanMessagesWithCursor(roomId, requesterId, cursorId,
          PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("채팅방이 삭제된 경우 getAll 호출 시 CHAT_ROOM_NOT_FOUND 에러가 발생한다")
    void getAll_DeletedRoom_ThrowsException() {
      // given
      Long roomId = 1L;
      Long requesterId = 2L;
      ChatRoom mockRoom = ChatRoom.create(10L);
      mockRoom.delete();

      when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

      // when & then
      assertThatThrownBy(
          () -> chatMessageService.getAll(roomId, requesterId, ParticipantRole.FAN, null, 20))
          .isInstanceOf(AppException.class)
          .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_ROOM_NOT_FOUND);
    }
  }
}
