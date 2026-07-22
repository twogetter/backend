package com.bubbletea.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatRoomWriterTest {

  @Mock
  private ChatRoomRepository chatRoomRepository;

  @Mock
  private ChatParticipantRepository chatParticipantRepository;

  @InjectMocks
  private ChatRoomWriter chatRoomWriter;

  @Test
  @DisplayName("채팅방을 생성할 때 아티스트 참여자도 자동으로 저장한다")
  void create_SavesRoomAndArtistParticipant() {
    // given
    Long artistId = 1L;
    ChatRoom chatRoom = ChatRoom.create(artistId);
    ReflectionTestUtils.setField(chatRoom, "id", 10L);
    when(chatRoomRepository.saveAndFlush(any(ChatRoom.class))).thenReturn(chatRoom);

    // when
    Long roomId = chatRoomWriter.create(artistId);

    // then
    assertThat(roomId).isEqualTo(10L);
    verify(chatRoomRepository, times(1)).saveAndFlush(any(ChatRoom.class));

    ArgumentCaptor<ChatParticipant> participantCaptor = ArgumentCaptor.forClass(
        ChatParticipant.class);
    verify(chatParticipantRepository, times(1)).save(participantCaptor.capture());

    ChatParticipant savedParticipant = participantCaptor.getValue();
    assertThat(savedParticipant.getRoomId()).isEqualTo(10L);
    assertThat(savedParticipant.getUserId()).isEqualTo(artistId);
    assertThat(savedParticipant.getRole()).isEqualTo(ParticipantRole.ARTIST);
    assertThat(savedParticipant.getStatus()).isEqualTo(ParticipantStatus.ACTIVE);
  }
}
