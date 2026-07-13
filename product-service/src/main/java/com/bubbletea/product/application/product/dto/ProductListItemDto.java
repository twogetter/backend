package com.bubbletea.product.application.product.dto;

import com.bubbletea.product.domain.product.Product;

public record ProductListItemDto(
    String id,
    String artistName,
    String groupName,
    String imageUrl,
    long price
) {

    public static ProductListItemDto from(Product product) {
        return new ProductListItemDto(
            product.getId(),
            product.getArtistName(),
            product.getGroupName(),
            product.getImageUrl(),
            product.getPrice()
        );
    }
}
