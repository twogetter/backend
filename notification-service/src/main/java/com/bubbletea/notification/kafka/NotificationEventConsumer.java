package com.bubbletea.notification.kafka;

import com.bubbletea.notification.kafka.dto.NotificationCreateEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${notification.kafka.topic}")
    public void consume(String message) {
        try {
            NotificationCreateEvent event = objectMapper.readValue(message, NotificationCreateEvent.class);
            if (!event.isValid()) {
                log.warn("Invalid notification event received. message={}", message);
                return;
            }

            log.info(
                    "Notification event received. eventId={}, notificationType={}, receiverIds={}",
                    event.eventId(),
                    event.notificationType(),
                    event.getReceiverIds()
            );
        } catch (JsonProcessingException exception) {
            log.warn("Failed to parse notification event. message={}", message, exception);
        }
    }
}
