package com.bubbletea.product.infrastructure.mongo;

import com.bubbletea.product.domain.product.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

interface ProductMongoRepository extends MongoRepository<Product, String> {

    boolean existsByArtistIdAndDeletedFalse(String artistId);
}
