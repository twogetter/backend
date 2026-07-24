package com.bubbletea.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.application.dto.ChatRoomResponseDto;
import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.ChatRoomStatus;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.repository.ChatMessageRepository;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.chat.domain.repository.dto.UnreadCountDto;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @Mock
  private ChatParticipantRepository chatParticipantRepository;

  @Mock
  private ChatMessageRepository chatMessageRepository;

  @Mock
  private ChatRoomReader chatRoomReader;

  @Mock
  private ChatRoomWriter chatRoomWriter;

  @InjectMocks
  private ChatRoomService chatRoomService;

  @Test
  @DisplayName("채팅방이 새로 개설될 때 ChatRoomService는 ChatRoomWriter를 호출한다")
  void createChatRoom_Success() {
    // given
    Long artistId = 1L;
    Long expectedRoomId = 10L;
    when(chatRoomWriter.create(artistId)).thenReturn(expectedRoomId);

    // when
    Long actualRoomId = chatRoomService.createChatRoom(artistId);

    // then
    assertThat(actualRoomId).isEqualTo(expectedRoomId);
    verify(chatRoomWriter, times(1)).create(artistId);
  }

  @Test
  @DisplayName("이미 존재하는 아티스트 채팅방 개설을 시도하면 기존 채팅방을 반환한다")
  void createChatRoom_Duplicate() {
    // given
    Long artistId = 1L;
    Long expectedRoomId = 10L;
    when(chatRoomWriter.create(artistId)).thenThrow(
        new DataIntegrityViolationException("Duplicate key",
            new RuntimeException("uk_chat_rooms_artist_id"))
    );
    when(chatRoomReader.getChatRoomId(artistId)).thenReturn(expectedRoomId);

    // when
    Long actualRoomId = chatRoomService.createChatRoom(artistId);

    // then
    assertThat(actualRoomId).isEqualTo(expectedRoomId);
    verify(chatRoomWriter, times(1)).create(artistId);
    verify(chatRoomReader, times(1)).getChatRoomId(artistId);
  }

  @Test
  @DisplayName("아티스트의 활성화된 채팅방 1개를 올바르게 조회한다")
  void getAll_Artist_Success() {
    // given
    Long userId = 1L;
    ParticipantRole role = ParticipantRole.ARTIST;

    ChatParticipant participant = ChatParticipant.createArtistParticipant(10L, userId);
    org.springframework.test.util.ReflectionTestUtils.setField(participant, "id", 100L);

    ChatRoom room = ChatRoom.create(userId);
    org.springframework.test.util.ReflectionTestUtils.setField(room, "id", 10L);

    when(chatParticipantRepository.findAllByUserIdAndRoleAndStatus(
        userId, role, ParticipantStatus.ACTIVE
    )).thenReturn(List.of(participant));

    when(chatRoomRepository.findAllByIdInAndStatus(
        List.of(10L), ChatRoomStatus.ACTIVE
    )).thenReturn(List.of(room));

    // when
    List<ChatRoomResponseDto> result = chatRoomService.getAll(userId, role);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().role()).isEqualTo(ParticipantRole.ARTIST);
    assertThat(result.getFirst().participantStatus()).isEqualTo(ParticipantStatus.ACTIVE);
    assertThat(result.getFirst().unreadCount()).isEqualTo(0L); // 아티스트는 항상 0L
  }

  @Test
  @DisplayName("팬이 참여 중인 활성화된 채팅방 목록을 모두 조회한다")
  void getAll_Fan_Success() {
    // given
    Long userId = 2L;
    ParticipantRole role = ParticipantRole.FAN;

    ChatParticipant participant1 = ChatParticipant.createFanParticipant(10L, userId);
    ChatParticipant participant2 = ChatParticipant.createFanParticipant(11L, userId);
    org.springframework.test.util.ReflectionTestUtils.setField(participant1, "id", 100L);
    org.springframework.test.util.ReflectionTestUtils.setField(participant2, "id", 101L);

    ChatRoom room1 = ChatRoom.create(1L);
    ChatRoom room2 = ChatRoom.create(3L);
    org.springframework.test.util.ReflectionTestUtils.setField(room1, "id", 10L);
    org.springframework.test.util.ReflectionTestUtils.setField(room2, "id", 11L);

    when(chatParticipantRepository.findAllByUserIdAndRoleAndStatus(
        userId, role, ParticipantStatus.ACTIVE
    )).thenReturn(List.of(participant1, participant2));

    when(chatRoomRepository.findAllByIdInAndStatus(
        List.of(10L, 11L), ChatRoomStatus.ACTIVE
    )).thenReturn(List.of(room1, room2));

    UnreadCountDto proj1 = mock(UnreadCountDto.class);
    when(proj1.getRoomId()).thenReturn(10L);
    when(proj1.getCount()).thenReturn(5L);

    UnreadCountDto proj2 = mock(UnreadCountDto.class);
    when(proj2.getRoomId()).thenReturn(11L);
    when(proj2.getCount()).thenReturn(2L);

    when(chatMessageRepository.countUnreadMessagesForFan(userId))
        .thenReturn(List.of(proj1, proj2));

    // when
    List<ChatRoomResponseDto> result = chatRoomService.getAll(userId, role);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).role()).isEqualTo(ParticipantRole.FAN);
    assertThat(result.get(0).unreadCount()).isEqualTo(5L);
    assertThat(result.get(1).role()).isEqualTo(ParticipantRole.FAN);
    assertThat(result.get(1).unreadCount()).isEqualTo(2L);
  }

  @Test
  @DisplayName("참여 중인 활성화된 채팅방이 존재하지 않으면 빈 목록을 반환한다")
  void getAll_Empty() {
    // given
    Long userId = 1L;
    ParticipantRole role = ParticipantRole.ARTIST;

    when(chatParticipantRepository.findAllByUserIdAndRoleAndStatus(
        userId, role, ParticipantStatus.ACTIVE
    )).thenReturn(List.of());

    // when
    List<ChatRoomResponseDto> result = chatRoomService.getAll(userId, role);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("DataIntegrityViolationException 발생 시 uk_chat_rooms_artist_id가 원인이 아니면 예외를 다시 던진다")
  void createChatRoom_OtherDataIntegrityViolation_ThrowsException() {
    // given
    Long artistId = 1L;
    DataIntegrityViolationException ex = new DataIntegrityViolationException("Other violation",
        new RuntimeException("some_other_constraint"));
    when(chatRoomWriter.create(artistId)).thenThrow(ex);

    // when & then
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> chatRoomService.createChatRoom(artistId))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("Other violation");
  }

  @Test
  @DisplayName("아티스트 ID로 채팅방을 정상 조회한다")
  void getChatRoomByArtistId_Success() {
    // given
    Long artistId = 1L;
    ChatRoom room = ChatRoom.create(artistId);
    when(chatRoomRepository.findByArtistId(artistId)).thenReturn(java.util.Optional.of(room));

    // when
    ChatRoom result = chatRoomService.getChatRoomByArtistId(artistId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getArtistId()).isEqualTo(artistId);
  }

  @Test
  @DisplayName("아티스트 ID로 채팅방 조회 시 없으면 예외가 발생한다")
  void getChatRoomByArtistId_NotFound() {
    // given
    Long artistId = 1L;
    when(chatRoomRepository.findByArtistId(artistId)).thenReturn(java.util.Optional.empty());

    // when & then
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> chatRoomService.getChatRoomByArtistId(artistId))
        .isInstanceOf(com.bubbletea.common.exception.AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", com.bubbletea.chat.domain.exception.ChatErrorCode.CHAT_ROOM_NOT_FOUND);
  }
}
