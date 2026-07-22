package com.bubbletea.product.infrastructure.mongo.product;

import com.bubbletea.product.domain.product.Product;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

interface ProductMongoRepository extends MongoRepository<Product, String> {

    Optional<Product> findByPid(Long pid);

    boolean existsByArtistIdAndDeletedFalse(Long artistId);
}
