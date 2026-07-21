package com.bubbletea.product.application.product.dto;

import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductStatus;
import java.time.LocalDateTime;

public record ProductDetailResponseDto(
    String id,
    Long pid,
    Long artistId,
    String artistName,
    String groupName,
    String name,
    String description,
    String imageUrl,
    long price,
    ProductStatus status,
    LocalDateTime openDate
) {

    public static ProductDetailResponseDto from(Product product) {
        return new ProductDetailResponseDto(
            product.getId(),
            product.getPid(),
            product.getArtistId(),
            product.getArtistName(),
            product.getGroupName(),
            product.getName(),
            product.getDescription(),
            product.getImageUrl(),
            product.getPrice(),
            product.getStatus(),
            product.getOpenDate()
        );
    }
}
