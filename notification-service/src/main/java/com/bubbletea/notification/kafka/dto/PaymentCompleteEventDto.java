package com.bubbletea.notification.kafka.dto;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.entity.enums.NotificationType;
import com.bubbletea.notification.kafka.support.NotificationEventPayload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentCompleteEventDto(
    String eventId,
    Long memberId,
    BigDecimal amount
) {

  public NotificationCreateCommandDto toCommand() {
    Map<String, Object> variables = NotificationEventPayload.of(
        "amount", amount
    );

    return new NotificationCreateCommandDto(
        eventId,
        memberId,
        NotificationType.PAYMENT_COMPLETE.name(),
        variables,
        null,
        variables
    );
  }
}
