package com.bubbletea.product.application.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "상품 목록 조회 응답")
public record ProductListResponseDto(
    List<ProductListItemDto> items,
    String nextCursor,
    boolean hasNext
) {

}
