package com.bubbletea.product.domain.outbox;

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
@Document(collection = "product_outbox_events")
@CompoundIndexes({
    @CompoundIndex(name = "idx_status_nextAttemptAt", def = "{'status': 1, 'nextAttemptAt': 1}")
})
public class OutboxEvent extends BaseTimeEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String idempotencyKey;

    private String topic;

    private String payload;

    private Map<String, String> headers;

    private OutboxEventStatus status;

    private int retryCount;

    private LocalDateTime claimedAt;

    private LocalDateTime nextAttemptAt;

    private LocalDateTime publishedAt;

    private String failReason;

    @Indexed(name = "ttl_expireAt", expireAfter = "1s")
    private LocalDateTime expireAt;

    private OutboxEvent(
        String idempotencyKey,
        String topic,
        String payload,
        Map<String, String> headers
    ) {
        this.idempotencyKey = idempotencyKey;
        this.topic = topic;
        this.payload = payload;
        this.headers = headers;
        this.status = OutboxEventStatus.PENDING;
        this.retryCount = 0;
    }

    public static OutboxEvent of(
        String idempotencyKey,
        String topic,
        String payload,
        Map<String, String> headers
    ) {
        return new OutboxEvent(idempotencyKey, topic, payload, headers);
    }
}
