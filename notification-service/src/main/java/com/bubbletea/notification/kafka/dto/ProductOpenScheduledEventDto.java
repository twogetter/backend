package com.bubbletea.notification.kafka.dto;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.entity.enums.NotificationType;
import com.bubbletea.notification.kafka.support.NotificationEventPayload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductOpenScheduledEventDto(
    String eventId,
    Long memberId,
    String productName,
    String schedule
) {

  public NotificationCreateCommandDto toCommand() {
    Map<String, Object> variables = NotificationEventPayload.of(
        "productName", productName,
        "schedule", schedule
    );

    return new NotificationCreateCommandDto(
        eventId,
        memberId,
        NotificationType.PRODUCT_OPEN_SCHEDULED.name(),
        variables,
        null,
        variables
    );
  }
}
