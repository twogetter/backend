package com.bubbletea.product.domain.product;


import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(String productId);

    boolean existsByArtistIdAndDeletedFalse(String artistId);
}
