package com.bubbletea.chat.presentation.controller;

import com.bubbletea.chat.application.dto.ChatRoomResponseDto;
import com.bubbletea.chat.application.service.ChatRoomService;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import com.bubbletea.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

  private final ChatRoomService chatRoomService;

  @GetMapping
  public ApiResponse<List<ChatRoomResponseDto>> list(
      @RequestHeader("X-User-Id") Long userId,
      @RequestHeader("X-User-Role") String roleHeader
  ) {
    ParticipantRole role = ParticipantRole.from(roleHeader);

    List<ChatRoomResponseDto> response = chatRoomService.getAll(userId, role);
    return ApiResponse.success(response);
  }
}
