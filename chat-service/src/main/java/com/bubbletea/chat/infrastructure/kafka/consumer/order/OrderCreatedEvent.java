package com.bubbletea.chat.infrastructure.kafka.consumer.order;

import java.time.LocalDateTime;

public record OrderCreatedEvent(Long fanId, Long artistId, LocalDateTime startedAt) {

}
