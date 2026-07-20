package com.bubbletea.chat.presentation.controller;

import com.bubbletea.chat.application.dto.ChatMessageResponseDto;
import com.bubbletea.chat.application.service.ChatMessageService;
import com.bubbletea.chat.domain.exception.ChatErrorCode;
import com.bubbletea.chat.infrastructure.security.SecurityContext;
import com.bubbletea.chat.infrastructure.security.SecurityContextHolder;
import com.bubbletea.chat.presentation.controller.dto.ChatMessageCreateRequestDto;
import com.bubbletea.common.exception.AppException;
import com.bubbletea.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/rooms")
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

  private final ChatMessageService chatMessageService;

  @PostMapping("/{roomId}/messages")
  public ApiResponse<ChatMessageResponseDto> create(
      @PathVariable Long roomId,
      @Valid @RequestBody ChatMessageCreateRequestDto requestDto
  ) {
    SecurityContext context = SecurityContextHolder.getContext();
    if (context == null || context.userId() == null || context.role() == null) {
      throw new AppException(ChatErrorCode.INVALID_ROLE);
    }

    ChatMessageResponseDto response = chatMessageService.save(
        roomId,
        context.userId(),
        context.role(),
        requestDto
    );

    return ApiResponse.success(response);
  }

  @GetMapping("/{roomId}/messages")
  public ApiResponse<List<ChatMessageResponseDto>> list(
      @PathVariable Long roomId,
      @RequestParam(required = false) Long cursorId,
      @RequestParam(defaultValue = "20") int size
  ) {
    SecurityContext context = SecurityContextHolder.getContext();
    if (context == null || context.userId() == null || context.role() == null) {
      throw new AppException(ChatErrorCode.INVALID_ROLE);
    }

    if (size < 1 || size > 100) {
      throw new AppException(ChatErrorCode.INVALID_SIZE);
    }

    List<ChatMessageResponseDto> response = chatMessageService.getAll(
        roomId,
        context.userId(),
        context.role(),
        cursorId,
        size
    );

    return ApiResponse.success(response);
  }
}
