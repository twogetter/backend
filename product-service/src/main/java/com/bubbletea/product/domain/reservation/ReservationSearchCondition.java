package com.bubbletea.product.domain.reservation;

public record ReservationSearchCondition(
    ReservationCategory category,
    ReservationStatus status
) {

    public boolean hasCategory() {
        return category != null;
    }

    public boolean hasStatus() {
        return status != null;
    }
}
