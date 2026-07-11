package com.bubbletea.product.domain.history;

import com.bubbletea.product.domain.common.BaseCreatedAtEntity;
import com.bubbletea.product.domain.reservation.ReservationCommandType;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "product_change_histories")
@CompoundIndexes({
    @CompoundIndex(name = "idx_productId_executedAt", def = "{'productId': 1, 'executedAt': -1}")
})
public class ProductChangeHistory extends BaseCreatedAtEntity {

    @Id
    private String id;

    private String reservationId;

    private String productId;

    private ReservationCommandType commandType;

    private Map<String, Object> appliedPayload;

    private String resultStatus;

    private String kafkaEventId;

    private LocalDateTime executedAt;

    private ProductChangeHistory(
        String reservationId, String productId, ReservationCommandType commandType,
        Map<String, Object> appliedPayload, String resultStatus,
        String kafkaEventId, LocalDateTime executedAt
    ) {
        this.reservationId = reservationId;
        this.productId = productId;
        this.commandType = commandType;
        this.appliedPayload = appliedPayload;
        this.resultStatus = resultStatus;
        this.kafkaEventId = kafkaEventId;
        this.executedAt = executedAt;
    }
}
