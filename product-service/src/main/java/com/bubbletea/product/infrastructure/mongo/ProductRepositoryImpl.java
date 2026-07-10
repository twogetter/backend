package com.bubbletea.product.infrastructure.mongo;

import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ProductRepositoryImpl implements ProductRepository {

    private final ProductMongoRepository productMongoRepository;

    @Override
    public Product save(Product product) {
        return productMongoRepository.save(product);
    }

    @Override
    public boolean existsByArtistIdAndDeletedFalse(String artistId) {
        return productMongoRepository.existsByArtistIdAndDeletedFalse(artistId);
    }
}
