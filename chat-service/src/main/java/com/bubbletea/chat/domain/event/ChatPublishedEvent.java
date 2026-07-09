package com.bubbletea.chat.domain.event;

import com.bubbletea.chat.domain.enums.MessageType;
import java.time.LocalDateTime;

public record ChatPublishedEvent(
    String productName,
    String messageData,
    MessageType messageType,
    LocalDateTime createdAt
) {

  public static ChatPublishedEvent of(String productName, String messageData,
      MessageType messageType, LocalDateTime createdAt) {
    return new ChatPublishedEvent(productName, messageData, messageType, createdAt);
  }
}
