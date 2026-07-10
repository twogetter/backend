package com.bubbletea.product.application.product.dto;

import com.bubbletea.product.domain.product.Product;

import java.time.LocalDateTime;

public record ProductResponseDto(
    String id,
    String name,
    String artistId,
    String artistName,
    String groupName,
    String status,
    long price,
    LocalDateTime openDate,
    LocalDateTime createdAt
) {

    public static ProductResponseDto from(Product product) {
        return new ProductResponseDto(
            product.getId(),
            product.getName(),
            product.getArtistId(),
            product.getArtistName(),
            product.getGroupName(),
            product.getStatus().name(),
            product.getPrice(),
            product.getOpenDate(),
            product.getCreatedAt()
        );
    }
}
