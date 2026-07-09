package com.bubbletea.payment.service.dto;

public record TossWebhookRequestDto(
        String eventType,
        String createdAt,
        WebhookData data
) {

    public record WebhookData (
            String customerKey,
            String status
    ) {
    }
}
