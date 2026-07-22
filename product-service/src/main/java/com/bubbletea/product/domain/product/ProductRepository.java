package com.bubbletea.product.domain.product;


import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(String productId);

    Optional<Product> findByPid(Long pid);

    boolean existsByArtistIdAndDeletedFalse(Long artistId);

    Optional<Product> findActiveProductById(String productId);

    List<Product> findActiveProducts(ProductListCondition condition);

    List<String> findDistinctActiveGroupNames();

    List<Product> searchActiveProducts(ProductSearchCondition condition);

}
