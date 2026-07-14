package com.bubbletea.product.application.product.dto;

import java.util.List;

public record ProductListResponseDto(
    List<ProductListItemDto> items,
    String nextCursor,
    boolean hasNext
) {

}
