package com.bubbletea.product.presentation.internal.dto;


import lombok.Builder;

@Builder
public record ProductResponseDto(
    Long price,
    String productStatus,
    Long artistId
) {

}
