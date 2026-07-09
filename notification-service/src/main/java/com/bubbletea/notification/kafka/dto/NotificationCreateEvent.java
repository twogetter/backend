package com.bubbletea.notification.kafka.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationCreateEvent(
        @JsonAlias("eventId")
        String eventId,
        String notificationType,
        Long receiverId,
        @JsonAlias("receivers")
        List<Long> receiverIds,
        Map<String, Object> variables,
        String linkUrl,
        Map<String, Object> payload
) {

    public Set<Long> getReceiverIds() {
        Set<Long> resolvedReceiverIds = new LinkedHashSet<>();

        if (receiverId != null) {
            resolvedReceiverIds.add(receiverId);
        }
        if (receiverIds != null) {
            resolvedReceiverIds.addAll(receiverIds);
        }

        return resolvedReceiverIds;
    }

    public boolean isValid() {
        return eventId != null
                && !eventId.isBlank()
                && notificationType != null
                && !notificationType.isBlank()
                && !getReceiverIds().isEmpty();
    }
}
