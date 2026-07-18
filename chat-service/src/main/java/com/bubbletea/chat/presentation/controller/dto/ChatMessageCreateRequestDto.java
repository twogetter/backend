package com.bubbletea.chat.presentation.controller.dto;

import com.bubbletea.chat.domain.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageCreateRequestDto(
    @NotBlank(message = "내용을 입력해 주세요.")
    String content,

    @NotNull(message = "메시지 타입은 필수입니다.")
    MessageType messageType
) {

}
