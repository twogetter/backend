package com.bubbletea.product.domain.history;

public interface ProductPriceChangeHistoryRepository {

    ProductPriceChangeHistory save(ProductPriceChangeHistory history);
}
