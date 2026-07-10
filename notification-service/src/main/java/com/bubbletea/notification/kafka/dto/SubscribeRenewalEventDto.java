package com.bubbletea.notification.kafka.dto;

import com.bubbletea.notification.application.dto.NotificationCreateCommandDto;
import com.bubbletea.notification.entity.enums.NotificationType;
import com.bubbletea.notification.kafka.support.NotificationEventPayload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SubscribeRenewalEventDto(
    String eventId,
    Long memberId,
    String productName,
    BigDecimal amount,
    Integer daysLeft,
    String renewalDate
) {

  public NotificationCreateCommandDto toCommand() {
    Map<String, Object> variables = NotificationEventPayload.of(
        "productName", productName,
        "amount", amount,
        "daysLeft", daysLeft,
        "renewalDate", renewalDate
    );

    return new NotificationCreateCommandDto(
        eventId,
        memberId,
        NotificationType.SUBSCRIBE_RENEWAL.name(),
        variables,
        null,
        variables
    );
  }
}
