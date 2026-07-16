package com.bubbletea.chat.presentation.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bubbletea.chat.application.dto.ChatRoomResponseDto;
import com.bubbletea.chat.application.service.ChatRoomService;
import com.bubbletea.chat.domain.enums.ChatRoomStatus;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import com.bubbletea.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ChatRoomControllerTest {

  private MockMvc mockMvc;

  @Mock
  private ChatRoomService chatRoomService;

  @BeforeEach
  void setUp() {
    ChatRoomController chatRoomController = new ChatRoomController(chatRoomService);
    mockMvc = MockMvcBuilders
        .standaloneSetup(chatRoomController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  @DisplayName("아티스트 권한을 통해 성공적으로 채팅방 목록을 조회한다")
  void list_Artist_Success() throws Exception {
    // given
    Long userId = 1L;
    List<ChatRoomResponseDto> mockResponse = List.of(
        new ChatRoomResponseDto(10L, userId, ChatRoomStatus.ACTIVE, null, ParticipantRole.ARTIST,
            ParticipantStatus.ACTIVE)
    );
    when(chatRoomService.getAll(userId, ParticipantRole.ARTIST)).thenReturn(mockResponse);

    // when & then
    mockMvc.perform(get("/api/chats/rooms")
            .header("X-User-Id", userId)
            .header("X-User-Role", "BUSINESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].roomId").value(10L))
        .andExpect(jsonPath("$.data[0].role").value("ARTIST"));
  }

  @Test
  @DisplayName("팬 권한을 통해 성공적으로 채팅방 목록을 조회한다")
  void list_Fan_Success() throws Exception {
    // given
    Long userId = 2L;
    List<ChatRoomResponseDto> mockResponse = List.of(
        new ChatRoomResponseDto(10L, 1L, ChatRoomStatus.ACTIVE, null, ParticipantRole.FAN,
            ParticipantStatus.ACTIVE)
    );
    when(chatRoomService.getAll(userId, ParticipantRole.FAN)).thenReturn(mockResponse);

    // when & then
    mockMvc.perform(get("/api/chats/rooms")
            .header("X-User-Id", userId)
            .header("X-User-Role", "USER"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].roomId").value(10L))
        .andExpect(jsonPath("$.data[0].role").value("FAN"));
  }

  @Test
  @DisplayName("잘못된 권한 헤더를 실어 보낼 경우 CHAT-INVALID-ROLE 에러를 반환한다")
  void list_InvalidRole() throws Exception {
    // given
    Long userId = 1L;

    // when & then
    mockMvc.perform(get("/api/chats/rooms")
            .header("X-User-Id", userId)
            .header("X-User-Role", "ADMIN"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.error").value("CHAT-INVALID-ROLE"));
  }

  @Test
  @DisplayName("필수 헤더가 누락될 경우 400 Bad Request 에러를 반환한다")
  void list_MissingHeader() throws Exception {
    // when & then
    mockMvc.perform(get("/api/chats/rooms")
            .header("X-User-Id", 1L))
        .andExpect(status().isBadRequest());
  }
}
