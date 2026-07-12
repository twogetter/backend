package com.bubbletea.product.infrastructure.mongo.history;

import com.bubbletea.product.domain.history.ProductChangeHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

interface ProductChangeHistoryMongoRepository extends MongoRepository<ProductChangeHistory, String> {
}
