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

    /**
     * 삭제 예약 생성
     */
    public static ProductChangeReservation deletion() {
        return ProductChangeReservation.ofDeletion(
            DEFAULT_PRODUCT_ID,
            DEFAULT_DELETION_DATE
        );
    }

    public static ProductChangeReservation deletion(
        String productId, LocalDateTime deletionDate
    ) {
        return ProductChangeReservation.ofDeletion(productId, deletionDate);
    }

    /**
     * 삭제 일정 알림 예약 생성
     */
    public static ProductChangeReservation notifyDeletionSchedule() {
        return ProductChangeReservation.ofNotifyDeletionSchedule(
            DEFAULT_PRODUCT_ID,
            DEFAULT_NOTIFY_AT,
            DEFAULT_PRODUCT_NAME,
            DEFAULT_DELETION_DATE
        );
    }

    public static ProductChangeReservation notifyDeletionSchedule(
        String productId, LocalDateTime notifyAt,
        String productName, LocalDateTime deletionDate
    ) {
        return ProductChangeReservation.ofNotifyDeletionSchedule(
            productId, notifyAt, productName, deletionDate);
    }

    /**
     * 오픈 일정 알림 예약 생성
     */
    public static ProductChangeReservation notifyOpenSchedule() {
        return ProductChangeReservation.ofNotifyOpenSchedule(
            DEFAULT_PRODUCT_ID,
            DEFAULT_NOTIFY_AT,
            DEFAULT_PRODUCT_NAME,
            DEFAULT_OPEN_DATE
        );
    }

    public static ProductChangeReservation notifyOpenSchedule(
        String productId, LocalDateTime notifyAt,
        String productName, LocalDateTime openDate
    ) {
        return ProductChangeReservation.ofNotifyOpenSchedule(
            productId, notifyAt, productName, openDate);
    }

    /**
     * 활성화 예약 생성
     */
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
