package com.bubbletea.chat.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bubbletea.chat.application.dto.ChatMessageResponseDto;
import com.bubbletea.chat.application.service.ChatMessageService;
import com.bubbletea.chat.domain.enums.MessageType;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.infrastructure.security.SecurityContextInterceptor;
import com.bubbletea.chat.presentation.controller.dto.ChatMessageCreateRequestDto;
import com.bubbletea.common.exception.AppException;
import com.bubbletea.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ChatMessageControllerTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockMvc mockMvc;
  @Mock
  private ChatMessageService chatMessageService;

  @BeforeEach
  void setUp() {
    ChatMessageController chatMessageController = new ChatMessageController(chatMessageService);
    mockMvc = MockMvcBuilders
        .standaloneSetup(chatMessageController)
        .setControllerAdvice(new GlobalExceptionHandler())
        .addInterceptors(new SecurityContextInterceptor())
        .build();
  }

  @Nested
  @DisplayName("메시지 전송")
  class CreateMessage {

    @Test
    @DisplayName("팬이 메시지를 전송하고 저장 결과를 반환받는다")
    void create_Fan_Success() throws Exception {
      // given
      Long roomId = 1L;
      Long userId = 2L;
      ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("ㅎㅇ",
          MessageType.TEXT);
      ChatMessageResponseDto responseDto = new ChatMessageResponseDto(100L, roomId, userId,
          ParticipantRole.FAN, "ㅎㅇ", MessageType.TEXT, LocalDateTime.now());

      when(chatMessageService.save(eq(roomId), eq(userId), eq(ParticipantRole.FAN),
          any(ChatMessageCreateRequestDto.class)))
          .thenReturn(responseDto);

      // when & then
      mockMvc.perform(post("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDto)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("SUCCESS"))
          .andExpect(jsonPath("$.data.id").value(100L))
          .andExpect(jsonPath("$.data.content").value("ㅎㅇ"))
          .andExpect(jsonPath("$.data.senderId").value(userId))
          .andExpect(jsonPath("$.data.senderType").value("FAN"));
    }

    @Test
    @DisplayName("아티스트가 메시지를 전송하고 저장 결과를 반환받는다")
    void create_Artist_Success() throws Exception {
      // given
      Long roomId = 1L;
      Long userId = 1L;
      ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("여러분",
          MessageType.TEXT);
      ChatMessageResponseDto responseDto = new ChatMessageResponseDto(101L, roomId, userId,
          ParticipantRole.ARTIST, "여러분", MessageType.TEXT, LocalDateTime.now());

      when(chatMessageService.save(eq(roomId), eq(userId), eq(ParticipantRole.ARTIST),
          any(ChatMessageCreateRequestDto.class)))
          .thenReturn(responseDto);

      // when & then
      mockMvc.perform(post("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "ARTIST")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDto)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("SUCCESS"))
          .andExpect(jsonPath("$.data.id").value(101L))
          .andExpect(jsonPath("$.data.content").value("여러분"))
          .andExpect(jsonPath("$.data.senderId").value(userId))
          .andExpect(jsonPath("$.data.senderType").value("ARTIST"));
    }

    @Test
    @DisplayName("참여자가 아니거나 비활성화 상태여서 검증 실패 시 PARTICIPANT_NOT_FOUND 에러를 반환한다")
    void create_NotParticipant() throws Exception {
      // given
      Long roomId = 1L;
      Long userId = 3L;
      ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("안녕",
          MessageType.TEXT);

      when(chatMessageService.save(eq(roomId), eq(userId), eq(ParticipantRole.FAN),
          any(ChatMessageCreateRequestDto.class)))
          .thenThrow(new AppException(ChatErrorCode.PARTICIPANT_NOT_FOUND));

      // when & then
      mockMvc.perform(post("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDto)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-NOTFOUND-PARTICIPANT"));
    }

    @Test
    @DisplayName("존재하지 않는 방이어서 검증 실패 시 CHAT_ROOM_NOT_FOUND 에러를 반환한다")
    void create_RoomNotFound() throws Exception {
      // given
      Long roomId = 999L;
      Long userId = 2L;
      ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("안녕",
          MessageType.TEXT);

      when(chatMessageService.save(eq(roomId), eq(userId), eq(ParticipantRole.FAN),
          any(ChatMessageCreateRequestDto.class)))
          .thenThrow(new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

      // when & then
      mockMvc.perform(post("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDto)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-NOTFOUND-ROOM"));
    }

    @Test
    @DisplayName("전송 횟수를 초과한 팬의 전송 요청 시 EXCEEDED_DAILY_LIMIT 에러를 반환한다")
    void create_ExceededDailyLimit() throws Exception {
      // given
      Long roomId = 1L;
      Long userId = 2L;
      ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("안녕",
          MessageType.TEXT);

      when(chatMessageService.save(eq(roomId), eq(userId), eq(ParticipantRole.FAN),
          any(ChatMessageCreateRequestDto.class)))
          .thenThrow(new AppException(ChatErrorCode.EXCEEDED_DAILY_LIMIT));

      // when & then
      mockMvc.perform(post("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDto)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-EXCEEDED-DAILY_LIMIT"));
    }

    @Test
    @DisplayName("필수 요청 헤더가 없거나 비정상적인 권한일 경우 에러를 반환한다")
    void create_InvalidRole() throws Exception {
      // given
      Long roomId = 1L;
      ChatMessageCreateRequestDto requestDto = new ChatMessageCreateRequestDto("안녕하세요",
          MessageType.TEXT);

      // when & then (헤더 누락)
      mockMvc.perform(post("/api/chats/rooms/{roomId}/messages", roomId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDto)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-INVALID-ROLE"));

      // when & then (잘못된 권한)
      mockMvc.perform(post("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", 1L)
              .header("X-User-Role", "XXXX")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(requestDto)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-INVALID-ROLE"));
    }
  }

  @Nested
  @DisplayName("메시지 내역 조회")
  class ListMessages {

    @Test
    @DisplayName("팬이 메시지 내역 조회를 호출하면 성공한다")
    void list_Fan_Success() throws Exception {
      // given
      Long roomId = 1L;
      Long userId = 2L;
      ChatMessageResponseDto msg = new ChatMessageResponseDto(100L, roomId, userId,
          ParticipantRole.FAN, "ㅎㅇ", MessageType.TEXT, LocalDateTime.now());

      when(chatMessageService.getAll(roomId, userId, ParticipantRole.FAN, null, 20))
          .thenReturn(List.of(msg));

      // when & then
      mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("SUCCESS"))
          .andExpect(jsonPath("$.data[0].id").value(100L))
          .andExpect(jsonPath("$.data[0].content").value("ㅎㅇ"))
          .andExpect(jsonPath("$.data[0].senderId").value(userId))
          .andExpect(jsonPath("$.data[0].senderType").value("FAN"));
    }

    @Test
    @DisplayName("아티스트가 메시지 내역 조회를 호출하면 성공한다")
    void list_Artist_Success() throws Exception {
      // given
      Long roomId = 1L;
      Long userId = 1L;
      Long cursorId = 150L;
      ChatMessageResponseDto msg = new ChatMessageResponseDto(140L, roomId, userId,
          ParticipantRole.ARTIST, "여러분", MessageType.TEXT, LocalDateTime.now());

      when(chatMessageService.getAll(roomId, userId, ParticipantRole.ARTIST, cursorId, 10))
          .thenReturn(List.of(msg));

      // when & then
      mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "ARTIST")
              .param("cursorId", String.valueOf(cursorId))
              .param("size", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("SUCCESS"))
          .andExpect(jsonPath("$.data[0].id").value(140L))
          .andExpect(jsonPath("$.data[0].content").value("여러분"))
          .andExpect(jsonPath("$.data[0].senderId").value(userId))
          .andExpect(jsonPath("$.data[0].senderType").value("ARTIST"));
    }

    @Test
    @DisplayName("인증 헤더가 없거나 비정상적인 권한일 경우 에러를 반환한다")
    void list_InvalidRole() throws Exception {
      Long roomId = 1L;

      // 헤더 누락
      mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-INVALID-ROLE"));

      // 잘못된 권한
      mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", 1L)
              .header("X-User-Role", "XXXX"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-INVALID-ROLE"));
    }

    @Test
    @DisplayName("가입되지 않은 참여자가 조회 요청 시 PARTICIPANT_NOT_FOUND 에러를 반환한다")
    void list_NotParticipant() throws Exception {
      Long roomId = 1L;
      Long userId = 3L;

      when(chatMessageService.getAll(roomId, userId, ParticipantRole.FAN, null, 20))
          .thenThrow(new AppException(ChatErrorCode.PARTICIPANT_NOT_FOUND));

      mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-NOTFOUND-PARTICIPANT"));
    }

    @Test
    @DisplayName("존재하지 않는 방 조회 시 CHAT_ROOM_NOT_FOUND 에러를 반환한다")
    void list_RoomNotFound() throws Exception {
      Long roomId = 999L;
      Long userId = 2L;

      when(chatMessageService.getAll(roomId, userId, ParticipantRole.FAN, null, 20))
          .thenThrow(new AppException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));

      mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-NOTFOUND-ROOM"));
    }

    @Test
    @DisplayName("size가 0 이하이거나 100을 초과할 경우 CHAT-INVALID-SIZE 에러를 반환한다")
    void list_InvalidSize() throws Exception {
      Long roomId = 1L;
      Long userId = 2L;

      // 0인 경우
      mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER")
              .param("size", "0"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-INVALID-SIZE"));

      // 음수인 경우
      mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER")
              .param("size", "-5"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-INVALID-SIZE"));

      // 100 초과인 경우
      mockMvc.perform(get("/api/chats/rooms/{roomId}/messages", roomId)
              .header("X-User-Id", userId)
              .header("X-User-Role", "USER")
              .param("size", "101"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.status").value("ERROR"))
          .andExpect(jsonPath("$.error").value("CHAT-INVALID-SIZE"));
    }
  }
}
