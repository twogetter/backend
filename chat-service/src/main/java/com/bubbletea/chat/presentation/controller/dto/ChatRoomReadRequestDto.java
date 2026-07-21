package com.bubbletea.chat.presentation.controller.dto;

import jakarta.validation.constraints.NotNull;

public record ChatRoomReadRequestDto(
    @NotNull(message = "마지막 읽은 메시지 ID는 필수입니다.")
    Long lastReadId
) {}
