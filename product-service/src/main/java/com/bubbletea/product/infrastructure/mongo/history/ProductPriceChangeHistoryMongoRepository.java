package com.bubbletea.product.infrastructure.mongo.history;

import com.bubbletea.product.domain.history.ProductPriceChangeHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

interface ProductPriceChangeHistoryMongoRepository extends MongoRepository<ProductPriceChangeHistory, String> {
}
