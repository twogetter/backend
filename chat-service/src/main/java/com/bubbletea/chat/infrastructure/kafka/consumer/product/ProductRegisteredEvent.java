package com.bubbletea.chat.infrastructure.kafka.consumer.product;

import java.time.LocalDateTime;

public record ProductRegisteredEvent(Long artistId, LocalDateTime registeredAt) {

}
