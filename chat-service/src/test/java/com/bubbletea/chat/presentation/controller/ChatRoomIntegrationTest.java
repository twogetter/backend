package com.bubbletea.chat.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.chat.presentation.controller.dto.ChatRoomReadRequestDto;
import com.bubbletea.chat.support.ChatIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class ChatRoomIntegrationTest extends ChatIntegrationTestSupport {

  @Autowired
  private ChatRoomRepository chatRoomRepository;

  @Autowired
  private ChatParticipantRepository chatParticipantRepository;

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
  }

  @AfterEach
  void tearDown() {
    chatParticipantRepository.deleteAllInBatch();
    chatRoomRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("사용자가 참여 중인 채팅방 목록을 조회한다.")
  void listChatRooms() throws Exception {
    // given
    long userId = 1L;
    String role = "ARTIST";

    // when
    ResultActions result = mockMvc.perform(get("/api/chats/rooms")
        .header("X-User-Id", Long.toString(userId))
        .header("X-User-Role", role)
        .contentType(MediaType.APPLICATION_JSON));

    // then
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].roomId").value(chatRoom.getId()))
        .andExpect(jsonPath("$.data[0].artistId").value(1L));
  }

  @Test
  @DisplayName("채팅방 읽음 처리를 수행하면 lastReadId가 갱신된다.")
  void updateLastReadId() throws Exception {
    // given
    Long userId = 1L;
    String role = "ARTIST";
    Long lastReadId = 100L;

    ChatRoomReadRequestDto requestDto = new ChatRoomReadRequestDto(lastReadId);

    // when
    ResultActions result = mockMvc.perform(patch("/api/chats/rooms/{roomId}/read", chatRoom.getId())
        .header("X-User-Id", userId.toString())
        .header("X-User-Role", role)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(requestDto)));

    // then
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"));

    ChatParticipant participant = chatParticipantRepository.findByRoomIdAndUserId(chatRoom.getId(),
        userId).orElseThrow();
    org.assertj.core.api.Assertions.assertThat(participant.getLastReadId()).isEqualTo(lastReadId);
  }
}
