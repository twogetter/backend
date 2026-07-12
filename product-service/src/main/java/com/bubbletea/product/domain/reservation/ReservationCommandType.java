package com.bubbletea.product.domain.reservation;

import lombok.AllArgsConstructor;


@AllArgsConstructor
public enum ReservationCommandType {

    NOTIFY_OPEN_SCHEDULE(ReservationCategory.NOTIFICATION),
    NOTIFY_DEACTIVATION_SCHEDULE(ReservationCategory.NOTIFICATION),
    NOTIFY_ACTIVATION_SCHEDULE(ReservationCategory.NOTIFICATION),
    NOTIFY_DELETION_SCHEDULE(ReservationCategory.NOTIFICATION),
    NOTIFY_PRICE_CHANGE_SCHEDULE(ReservationCategory.NOTIFICATION),

    ACTIVATE(ReservationCategory.STATUS_CHANGE),
    DEACTIVATE(ReservationCategory.STATUS_CHANGE),
    DELETION(ReservationCategory.STATUS_CHANGE),

    PRICE_CHANGE(ReservationCategory.PRICE_CHANGE);

    private final ReservationCategory category;

    public ReservationCategory category() {
        return category;
    }

}
