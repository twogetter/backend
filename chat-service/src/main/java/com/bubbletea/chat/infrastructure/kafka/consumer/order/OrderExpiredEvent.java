package com.bubbletea.chat.infrastructure.kafka.consumer.order;

import java.time.LocalDateTime;

public record OrderExpiredEvent(Long fanId, Long artistId, LocalDateTime endedAt) {

}
