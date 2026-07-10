package com.bubbletea.product.domain.product;


public interface ProductRepository {

    Product save(Product product);

    boolean existsByArtistIdAndDeletedFalse(String artistId);
}
