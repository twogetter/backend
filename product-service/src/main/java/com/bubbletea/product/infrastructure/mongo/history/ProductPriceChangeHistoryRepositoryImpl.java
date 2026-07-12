package com.bubbletea.product.infrastructure.mongo.history;

import com.bubbletea.product.domain.history.ProductPriceChangeHistory;
import com.bubbletea.product.domain.history.ProductPriceChangeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProductPriceChangeHistoryRepositoryImpl implements ProductPriceChangeHistoryRepository {

    private final ProductPriceChangeHistoryMongoRepository productPriceChangeHistoryMongoRepository;

    @Override
    public ProductPriceChangeHistory save(ProductPriceChangeHistory history) {
        return productPriceChangeHistoryMongoRepository.save(history);
    }
}
