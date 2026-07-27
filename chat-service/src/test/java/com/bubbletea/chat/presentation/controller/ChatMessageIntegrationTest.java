package com.bubbletea.chat.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bubbletea.chat.domain.entity.ChatMessage;
import com.bubbletea.chat.domain.entity.ChatParticipant;
import com.bubbletea.chat.domain.entity.ChatRoom;
import com.bubbletea.chat.domain.enums.MessageType;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.chat.domain.repository.ChatMessageRepository;
import com.bubbletea.chat.domain.repository.ChatParticipantRepository;
import com.bubbletea.chat.domain.repository.ChatRoomRepository;
import com.bubbletea.chat.presentation.controller.dto.ChatMessageCreateRequestDto;
import com.bubbletea.chat.support.ChatIntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class ChatMessageIntegrationTest extends ChatIntegrationTestSupport {

  @Autowired
  private ChatRoomRepository chatRoomRepository;

  @Autowired
  private ChatParticipantRepository chatParticipantRepository;

  @Autowired
  private ChatMessageRepository chatMessageRepository;

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
    chatMessageRepository.deleteAllInBatch();
    chatParticipantRepository.deleteAllInBatch();
    chatRoomRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("채팅방에 새로운 메시지를 전송한다.")
  void createMessage() throws Exception {
    // given
    long userId = 1L;
    String role = "ARTIST";
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto(
        "여러분",
        MessageType.TEXT
    );

    // when
    ResultActions result = mockMvc.perform(
        post("/api/chats/rooms/{roomId}/messages", chatRoom.getId())
            .header("X-User-Id", Long.toString(userId))
            .header("X-User-Role", role)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)));

    // then
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.content").value("여러분"))
        .andExpect(jsonPath("$.data.messageType").value("TEXT"));

    org.assertj.core.api.Assertions.assertThat(chatMessageRepository.findAll()).hasSize(1);
  }

  @Test
  @DisplayName("커서를 기반으로 채팅 메시지 목록을 조회한다.")
  void listMessages() throws Exception {
    // given
    long userId = 1L;
    String role = "ARTIST";

    chatMessageRepository.save(ChatMessage.builder()
        .roomId(chatRoom.getId())
        .senderId(1L)
        .senderType(ParticipantRole.ARTIST)
        .content("첫 번째 메시지")
        .messageType(MessageType.TEXT)
        .build());

    ChatMessage secondMessage = chatMessageRepository.save(ChatMessage.builder()
        .roomId(chatRoom.getId())
        .senderId(1L)
        .senderType(ParticipantRole.ARTIST)
        .content("두 번째 메시지")
        .messageType(MessageType.TEXT)
        .build());

    // when
    ResultActions result = mockMvc.perform(
        get("/api/chats/rooms/{roomId}/messages", chatRoom.getId())
            .header("X-User-Id", Long.toString(userId))
            .header("X-User-Role", role)
            .param("size", "10")
            .contentType(MediaType.APPLICATION_JSON));

    // then
    result.andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].id").value(secondMessage.getId()));
  }

  @Test
  @DisplayName("참여하지 않은 채팅방에 메시지를 전송하려 하면 예외가 발생한다.")
  void createMessage_Unauthorized() throws Exception {
    // given
    long unauthorizedUserId = 999L;
    String role = "FAN";
    ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto(
        "몰래 메시지 보내기",
        MessageType.TEXT
    );

    // when
    ResultActions result = mockMvc.perform(
        post("/api/chats/rooms/{roomId}/messages", chatRoom.getId())
            .header("X-User-Id", Long.toString(unauthorizedUserId))
            .header("X-User-Role", role)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestDto)));

    // then
    result.andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("CHAT-NOTFOUND-PARTICIPANT"));
  }
}
