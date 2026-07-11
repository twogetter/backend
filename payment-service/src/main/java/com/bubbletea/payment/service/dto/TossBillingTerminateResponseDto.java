package com.bubbletea.payment.service.dto;

public record TossBillingTerminateResponseDto(
        String code,
        String message
) {
    public TossBillingTerminateResponseDto() {
        this(null, null);
    }

    public boolean isSuccess() {
        return this.code == null;
    }
}