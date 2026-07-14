package com.bubbletea.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
}
