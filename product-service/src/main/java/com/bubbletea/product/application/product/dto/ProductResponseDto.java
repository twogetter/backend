package com.bubbletea.product.application.product.dto;

import com.bubbletea.product.domain.product.Product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "상품 등록 응답")
public record ProductResponseDto(
    String id,
    String name,
    Long artistId,
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
