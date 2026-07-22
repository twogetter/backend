package com.bubbletea.product.infrastructure.mongo;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bubbletea.product.domain.outbox.OutboxEvent;
import com.bubbletea.product.support.ProductMongoOnlySupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;


@DataMongoTest
public class MongoDuplicateIndexTest extends ProductMongoOnlySupport {

    @BeforeEach
    void setUpIndexes() {
        var resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
        resolver.resolveIndexFor(OutboxEvent.class)
            .forEach(def ->
                mongoTemplate.indexOps(OutboxEvent.class).createIndex(def));
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoMappingContext mongoMappingContext;

    @Test
    @DisplayName("outboxEvent: 동일한 idempotencyKey로 중복 저장 시 예외가 발생한다.")
    void outboxEvent_duplicateIdempotencyKey_throwsException() {
        // given
        String sameKey = "unique-idempotency-1234";

        OutboxEvent firstEvent = OutboxEvent.of(
            sameKey, "topic", "message", Map.of()
        );
        mongoTemplate.save(firstEvent);

        OutboxEvent secondEvent = OutboxEvent.of(
            sameKey, "topic", "message", Map.of()
        );

        // when & then
        DuplicateKeyException ex = assertThrows(
            DuplicateKeyException.class,
            () -> mongoTemplate.save(secondEvent)
        );

        assertThat(ex.getMessage())
            .contains("idempotencyKey")
            .contains("E11000");
    }

}
