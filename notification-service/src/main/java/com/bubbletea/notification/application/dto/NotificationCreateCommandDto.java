package com.bubbletea.notification.application.dto;

import java.util.Map;

public record NotificationCreateCommandDto(
    String eventId,
    Long receiverId,
    String notificationType,
    Map<String, Object> variables,
    String linkUrl,
    Map<String, Object> payload
) {

  public boolean isValid() {
    return eventId != null
        && !eventId.isBlank()
        && receiverId != null
        && receiverId > 0
        && notificationType != null
        && !notificationType.isBlank();
  }
}
