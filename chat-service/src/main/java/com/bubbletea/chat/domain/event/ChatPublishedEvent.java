package com.bubbletea.chat.domain.event;

import com.bubbletea.chat.domain.enums.MessageType;
import java.time.LocalDateTime;

public record ChatPublishedEvent(
    String eventId,
    Long memberId,
    String productName,
    String message,
    MessageType messageType,
    LocalDateTime sendedAt
) {

  public static ChatPublishedEvent of(
      String eventId,
      Long memberId,
      String productName,
      String message,
      MessageType messageType,
      LocalDateTime sendedAt
  ) {
    return new ChatPublishedEvent(eventId, memberId, productName, message, messageType, sendedAt);
  }
}
