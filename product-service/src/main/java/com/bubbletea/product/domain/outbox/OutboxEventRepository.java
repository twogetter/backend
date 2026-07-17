package com.bubbletea.product.domain.outbox;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> claimNextPending();

    void markPublished(String id);

    void markFailed(String id, String failReason);

    int recoverStalledPublishing(LocalDateTime staleBefore);
}
