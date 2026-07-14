package com.bubbletea.product.domain.reservation;

import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.DELETION_DATE;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.OPEN_DATE;
import static com.bubbletea.product.domain.reservation.ReservationPayloadKeys.PRODUCT_NAME;

import com.bubbletea.product.domain.common.BaseTimeEntity;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "product_change_reservations")
@CompoundIndexes({
    @CompoundIndex(name = "idx_status_scheduledAt", def = "{'status': 1, 'scheduledAt': 1}"),
    @CompoundIndex(name = "idx_productId_status", def = "{'productId': 1, 'status': 1}")
})
public class ProductChangeReservation extends BaseTimeEntity {

    @Id
    private String id;

    private String productId;

    private ReservationCommandType commandType;

    private Map<String, Object> payload;

    private ReservationStatus status;

    private LocalDateTime scheduledAt;

    private LocalDateTime claimedAt;

    private LocalDateTime executedAt;

    private String failReason;

    @Indexed(name = "ttl_expireAt, expireAfterSeconds = 0")
    private LocalDateTime expireAt;

    private ProductChangeReservation(
        String productId,
        ReservationCommandType commandType,
        Map<String, Object> payload,
        LocalDateTime scheduledAt
    ) {
        this.productId = productId;
        this.commandType = commandType;
        this.payload = payload;
        this.status = ReservationStatus.PENDING;
        this.scheduledAt = scheduledAt;
    }

    public static ProductChangeReservation ofNotifyOpenSchedule(
        String productId, LocalDateTime notifyAt,
        String productName, LocalDateTime openDate
    ) {
        return new ProductChangeReservation(
            productId,
            ReservationCommandType.NOTIFY_OPEN_SCHEDULE,
            Map.of(PRODUCT_NAME, productName, OPEN_DATE, openDate.toString()),
            notifyAt
        );
    }

    public static ProductChangeReservation ofActivate(
        String productId, LocalDateTime openDate
    ) {
        return new ProductChangeReservation(
            productId,
            ReservationCommandType.ACTIVATE,
            Map.of(),
            openDate
        );
    }

    public static ProductChangeReservation ofNotifyDeletionSchedule(
        String productId,
        LocalDateTime notifyAt,
        String productName,
        LocalDateTime deletionDate
    ) {
        return new ProductChangeReservation(
            productId,
            ReservationCommandType.NOTIFY_DELETION_SCHEDULE,
            Map.of(PRODUCT_NAME, productName, DELETION_DATE, deletionDate.toString()),
            notifyAt
        );
    }

    public static ProductChangeReservation ofDeletion(
        String productId, LocalDateTime deletionDate
    ) {
        return new ProductChangeReservation(
            productId,
            ReservationCommandType.DELETION,
            Map.of(),
            deletionDate
        );
    }

}
