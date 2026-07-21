package com.bubbletea.chat.presentation.controller;

import com.bubbletea.chat.application.dto.ChatRoomResponseDto;
import com.bubbletea.chat.application.service.ChatParticipantService;
import com.bubbletea.chat.application.service.ChatRoomService;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.infrastructure.security.SecurityContext;
import com.bubbletea.chat.infrastructure.security.SecurityContextHolder;
import com.bubbletea.chat.presentation.controller.dto.ChatRoomReadRequestDto;
import com.bubbletea.common.exception.AppException;
import com.bubbletea.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

  private final ChatRoomService chatRoomService;
  private final ChatParticipantService chatParticipantService;

  @GetMapping
  public ApiResponse<List<ChatRoomResponseDto>> list(
      @RequestHeader("X-User-Id") Long userId,
      @RequestHeader("X-User-Role") String roleHeader
  ) {
    ParticipantRole role = ParticipantRole.from(roleHeader);

    List<ChatRoomResponseDto> response = chatRoomService.getAll(userId, role);
    return ApiResponse.success(response);
  }

  @PatchMapping("/{roomId}/read")
  public ApiResponse<Void> read(
      @PathVariable Long roomId,
      @Valid @RequestBody ChatRoomReadRequestDto requestDto
  ) {
    SecurityContext context = SecurityContextHolder.getContext();
    if (context == null || context.userId() == null) {
      throw new AppException(ChatErrorCode.INVALID_ROLE);
    }

    chatParticipantService.updateLastReadId(roomId, context.userId(), requestDto.lastReadId());
    return ApiResponse.success(null);
  }
}
