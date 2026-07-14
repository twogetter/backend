package com.bubbletea.notification.service.event;

import com.bubbletea.notification.service.dto.NotificationResponseDto;

public record NotificationCreatedEvent(
    Long receiverId,
    NotificationResponseDto notification
) {
}
