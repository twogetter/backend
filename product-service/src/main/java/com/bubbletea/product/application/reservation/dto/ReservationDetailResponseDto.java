package com.bubbletea.product.application.reservation.dto;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationCategory;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import java.time.LocalDateTime;
import java.util.Map;

public record ReservationDetailResponseDto(
    String id,
    String productId,
    ReservationCommandType commandType,
    ReservationCategory category,
    Map<String, Object> payload,
    ReservationStatus status,
    LocalDateTime scheduledAt,
    LocalDateTime claimedAt,
    LocalDateTime executedAt,
    String failReason
) {
    public static ReservationDetailResponseDto from(ProductChangeReservation reservation) {
        return new ReservationDetailResponseDto(
            reservation.getId(),
            reservation.getProductId(),
            reservation.getCommandType(),
            reservation.getCommandType().category(),
            reservation.getPayload(),
            reservation.getStatus(),
            reservation.getScheduledAt(),
            reservation.getClaimedAt(),
            reservation.getExecutedAt(),
            reservation.getFailReason()
        );

    }
}
