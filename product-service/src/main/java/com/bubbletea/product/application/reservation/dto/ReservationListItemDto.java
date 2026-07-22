package com.bubbletea.product.application.reservation.dto;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import com.bubbletea.product.domain.reservation.ReservationCategory;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import java.time.LocalDateTime;

public record ReservationListItemDto(
    String id,
    String productId,
    ReservationCommandType commandType,
    ReservationCategory category,
    ReservationStatus status,
    LocalDateTime scheduledAt
) {

    public static ReservationListItemDto from(ProductChangeReservation reservation) {
        return new ReservationListItemDto(
            reservation.getId(),
            reservation.getProductId(),
            reservation.getCommandType(),
            reservation.getCommandType().category(),
            reservation.getStatus(),
            reservation.getScheduledAt()
        );
    }
}
