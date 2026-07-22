package com.bubbletea.product.infrastructure.mongo.outbox;

import com.bubbletea.product.domain.outbox.OutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

interface OutboxEventMongoRepository extends MongoRepository<OutboxEvent, String> {
}
