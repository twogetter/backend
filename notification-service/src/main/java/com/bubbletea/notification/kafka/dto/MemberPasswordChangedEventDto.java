package com.bubbletea.notification.kafka.dto;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.entity.enums.NotificationType;
import com.bubbletea.notification.kafka.support.NotificationEventPayload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberPasswordChangedEventDto(
    String eventId,
    String eventType,
    Long memberId,
    String occurredAt
) {

  public NotificationCreateCommandDto toCommand() {
    Map<String, Object> variables = NotificationEventPayload.of(
        "occurredAt", occurredAt
    );
    Map<String, Object> payload = NotificationEventPayload.of(
        "eventType", eventType,
        "occurredAt", occurredAt
    );

    return new NotificationCreateCommandDto(
        eventId,
        memberId,
        NotificationType.MEMBER_PASSWORD_CHANGED.name(),
        variables,
        null,
        payload
    );
  }
}
