package com.bubbletea.product.domain.reservation;

import java.time.LocalDateTime;

public class ProductChangeReservationFixture {

    public static final String DEFAULT_RESERVATION_ID = "reservation-1";
    public static final String DEFAULT_PRODUCT_ID = "product001";
    public static final String DEFAULT_PRODUCT_NAME = "[김연옌] 구독권";
    public static final LocalDateTime DEFAULT_SCHEDULED_AT =
        LocalDateTime.of(2026, 7, 23, 12, 0);
    public static final LocalDateTime DEFAULT_DELETION_DATE =
        LocalDateTime.of(2026, 7, 23, 12, 0);
    public static final LocalDateTime DEFAULT_NOTIFY_AT =
        DEFAULT_DELETION_DATE.minusDays(3);
    public static final LocalDateTime DEFAULT_OPEN_DATE =
        LocalDateTime.of(2026, 7, 23, 12, 0);


    public static ProductChangeReservation deletion() {
        return ProductChangeReservation.ofDeletion(
            DEFAULT_PRODUCT_ID,
            DEFAULT_DELETION_DATE
        );
    }

    public static ProductChangeReservation createByCommandType(
        ReservationCommandType commandType, LocalDateTime scheduledAt
    ) {
        return switch (commandType) {
            case ACTIVATE -> ProductChangeReservation.ofActivate(DEFAULT_PRODUCT_ID, scheduledAt);
            case DELETION -> ProductChangeReservation.ofDeletion(DEFAULT_PRODUCT_ID, scheduledAt);
            case NOTIFY_OPEN_SCHEDULE -> ProductChangeReservation.ofNotifyOpenSchedule(
                DEFAULT_PRODUCT_ID, scheduledAt, DEFAULT_PRODUCT_NAME, scheduledAt.plusDays(3));
            case NOTIFY_DELETION_SCHEDULE -> ProductChangeReservation.ofNotifyDeletionSchedule(
                DEFAULT_PRODUCT_ID, scheduledAt, DEFAULT_PRODUCT_NAME, scheduledAt.plusDays(3));
            default -> throw new IllegalArgumentException("지원하지 않는 commandType: " + commandType);
        };
    }

    public static ProductChangeReservation deletion(
        String productId, LocalDateTime deletionDate
    ) {
        return ProductChangeReservation.ofDeletion(productId, deletionDate);
    }

    public static ProductChangeReservation activate() {
        return ProductChangeReservation.ofActivate(
            DEFAULT_PRODUCT_ID,
            DEFAULT_OPEN_DATE
        );
    }

    public static ProductChangeReservation activate(
        String productId, LocalDateTime openDate
    ) {
        return ProductChangeReservation.ofActivate(productId, openDate);
    }

    private ProductChangeReservationFixture() {
    }
}
