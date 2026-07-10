package com.bubbletea.notification.kafka.dto;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.entity.enums.NotificationType;
import com.bubbletea.notification.kafka.support.NotificationEventPayload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MemberSignedUpEventDto(
    String eventId,
    String eventType,
    Long memberId,
    String email,
    String nickname,
    String occurredAt
) {

  public NotificationCreateCommandDto toCommand() {
    Map<String, Object> variables = NotificationEventPayload.of(
        "nickname", nickname,
        "occurredAt", occurredAt
    );
    Map<String, Object> payload = NotificationEventPayload.of(
        "eventType", eventType,
        "nickname", nickname,
        "occurredAt", occurredAt
    );

    return new NotificationCreateCommandDto(
        eventId,
        memberId,
        NotificationType.MEMBER_SIGNED_UP.name(),
        variables,
        null,
        payload
    );
  }
}
