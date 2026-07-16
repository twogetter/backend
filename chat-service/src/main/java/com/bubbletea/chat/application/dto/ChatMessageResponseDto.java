package com.bubbletea.chat.application.dto;

import com.bubbletea.chat.domain.entity.ChatMessage;
import com.bubbletea.chat.domain.enums.MessageType;
import com.bubbletea.chat.domain.enums.ParticipantRole;
import java.time.LocalDateTime;

public record ChatMessageResponseDto(
    Long id,
    Long roomId,
    Long senderId,
    ParticipantRole senderType,
    String content,
    MessageType messageType,
    LocalDateTime createdAt
) {

  public static ChatMessageResponseDto from(ChatMessage message) {
    return new ChatMessageResponseDto(
        message.getId(),
        message.getRoomId(),
        message.getSenderId(),
        message.getSenderType(),
        message.getContent(),
        message.getMessageType(),
        message.getCreatedAt()
    );
  }
}
