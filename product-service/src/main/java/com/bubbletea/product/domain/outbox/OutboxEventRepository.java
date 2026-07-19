package com.bubbletea.product.domain.outbox;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> claimNextPending();

    void markPublished(String id, LocalDateTime claimedAt);

    void markFailed(String id, String failReason, LocalDateTime claimedAt);

    void markPendingForRetry(
        String id, String failReason, LocalDateTime nextAttemptAt, LocalDateTime claimedAt);

    int recoverStalledPublishing(LocalDateTime staleBefore);
}
