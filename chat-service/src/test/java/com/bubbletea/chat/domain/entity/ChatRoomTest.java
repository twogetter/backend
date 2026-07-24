package com.bubbletea.chat.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.chat.domain.enums.ChatRoomStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChatRoomTest {

  @Test
  @DisplayName("채팅방을 생성하면 초기 상태는 ACTIVE이다")
  void create_ChatRoom() {
    // given
    Long artistId = 10L;

    // when
    ChatRoom chatRoom = ChatRoom.create(artistId);

    // then
    assertThat(chatRoom.getArtistId()).isEqualTo(artistId);
    assertThat(chatRoom.getStatus()).isEqualTo(ChatRoomStatus.ACTIVE);
  }

  @Test
  @DisplayName("채팅방을 삭제하면 상태가 DELETED로 변경된다")
  void delete_ChatRoom() {
    // given
    Long artistId = 10L;
    ChatRoom chatRoom = ChatRoom.create(artistId);

    // when
    chatRoom.delete();

    // then
    assertThat(chatRoom.getStatus()).isEqualTo(ChatRoomStatus.DELETED);
  }
}
