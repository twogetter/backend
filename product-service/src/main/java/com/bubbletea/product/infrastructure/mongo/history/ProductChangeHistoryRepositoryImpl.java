package com.bubbletea.product.infrastructure.mongo.history;

import com.bubbletea.product.domain.history.ProductChangeHistory;
import com.bubbletea.product.domain.history.ProductChangeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProductChangeHistoryRepositoryImpl implements ProductChangeHistoryRepository {

    private final ProductChangeHistoryMongoRepository productChangeHistoryMongoRepository;

    @Override
    public ProductChangeHistory save(ProductChangeHistory history) {
        return productChangeHistoryMongoRepository.save(history);
    }
}
