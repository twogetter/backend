package com.bubbletea.notification.kafka.dto;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.entity.enums.NotificationType;
import com.bubbletea.notification.kafka.support.NotificationEventPayload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatPublishedEventDto(
    String eventId,
    Long memberId,
    String productName,
    String message,
    String messageType,
    String sendedAt
) {

  public NotificationCreateCommandDto toCommand() {
    Map<String, Object> variables = NotificationEventPayload.of(
        "productName", productName,
        "message", message,
        "messageType", messageType,
        "sendedAt", sendedAt
    );

    return new NotificationCreateCommandDto(
        eventId,
        memberId,
        NotificationType.CHAT_PUBLISHED.name(),
        variables,
        null,
        variables
    );
  }
}
