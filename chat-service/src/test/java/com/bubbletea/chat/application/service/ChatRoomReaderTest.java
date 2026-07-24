package com.bubbletea.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.common.exception.AppException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatRoomReaderTest {

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @InjectMocks
  private ChatRoomReader chatRoomReader;

  @Test
  @DisplayName("아티스트 ID로 채팅방 ID를 정상 조회한다")
  void getChatRoomId_Success() {
    // given
    Long artistId = 1L;
    ChatRoom mockRoom = ChatRoom.create(artistId);
    ReflectionTestUtils.setField(mockRoom, "id", 100L);

    when(chatRoomRepository.findByArtistId(artistId)).thenReturn(Optional.of(mockRoom));

    // when
    Long roomId = chatRoomReader.getChatRoomId(artistId);

    // then
    assertThat(roomId).isEqualTo(100L);
  }

  @Test
  @DisplayName("채팅방이 존재하지 않으면 예외가 발생한다")
  void getChatRoomId_NotFound() {
    // given
    Long artistId = 1L;
    when(chatRoomRepository.findByArtistId(artistId)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> chatRoomReader.getChatRoomId(artistId))
        .isInstanceOf(AppException.class)
        .hasFieldOrPropertyWithValue("errorCode", ChatErrorCode.CHAT_ROOM_NOT_FOUND);
  }
}
