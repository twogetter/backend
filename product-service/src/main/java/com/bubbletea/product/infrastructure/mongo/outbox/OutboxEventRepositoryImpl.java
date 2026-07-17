package com.bubbletea.product.infrastructure.mongo.outbox;

import com.bubbletea.product.domain.outbox.OutboxEvent;
import com.bubbletea.product.domain.outbox.OutboxEventRepository;
import com.bubbletea.product.domain.outbox.OutboxEventStatus;
import com.mongodb.client.result.UpdateResult;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private static final long PUBLISHED_RETENTION_DAYS = 7L;
    private static final long FAILED_RETENTION_DAYS = 30L;

    private final OutboxEventMongoRepository outboxEventMongoRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return outboxEventMongoRepository.save(event);
    }

    @Override
    public Optional<OutboxEvent> claimNextPending() {
        Query query = Query.query(Criteria.where("status").is(OutboxEventStatus.PENDING))
            .with(Sort.by(Sort.Direction.ASC, "createdAt"))
            .limit(1);

        Update update = Update.update("status", OutboxEventStatus.PUBLISHING)
            .set("claimedAt", LocalDateTime.now());

        OutboxEvent claimed = mongoTemplate.findAndModify(
            query, update, FindAndModifyOptions.options().returnNew(true),
            OutboxEvent.class
        );

        return Optional.ofNullable(claimed);
    }

    @Override
    public void markPublished(String id) {
        Update update = Update.update("status", OutboxEventStatus.PUBLISHED)
            .set("publishedAt", LocalDateTime.now())
            .set("expireAt", LocalDateTime.now().plusDays(PUBLISHED_RETENTION_DAYS));

        applyGuardedByPublishing(id, update, "markPublished");
    }

    @Override
    public void markFailed(String id, String failReason) {
        Update update = Update.update("status", OutboxEventStatus.FAILED)
            .set("failReason", failReason)
            .set("expireAt", LocalDateTime.now().plusDays(FAILED_RETENTION_DAYS));

        applyGuardedByPublishing(id, update, "markFailed");
    }

    @Override
    public void markPendingForRetry(String id, String failReason) {
        Update update = Update.update("status", OutboxEventStatus.PENDING)
            .set("failReason", failReason)
            .inc("retryCount", 1)
            .unset("claimedAt");

        applyGuardedByPublishing(id, update, "markPendingForRetry");
    }

    @Override
    public int recoverStalledPublishing(LocalDateTime staleBefore) {
        Query query = Query.query(
            Criteria.where("status").is(OutboxEventStatus.PUBLISHING)
                .and("claimedAt").lt(staleBefore));

        Update update = Update.update("status", OutboxEventStatus.PENDING)
            .unset("claimedAt");

        return (int) mongoTemplate
            .updateMulti(query, update, OutboxEvent.class)
            .getModifiedCount();
    }

    private void applyGuardedByPublishing(String id, Update update, String operationName) {
        Query query = Query.query(Criteria.where("id").is(id)
            .and("status")
            .is(OutboxEventStatus.PUBLISHING));

        UpdateResult result = mongoTemplate.updateFirst(query, update, OutboxEvent.class);

        if (result.getModifiedCount() == 0) {
            log.warn("[outbox] {} 무시됨 outboxEventId={}", operationName, id);
        }
    }
}
