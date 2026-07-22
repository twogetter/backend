package com.bubbletea.product.application.product.dto;

import com.bubbletea.product.domain.product.Product;
import com.bubbletea.product.domain.product.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "상품 상세 조회 응답")
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
