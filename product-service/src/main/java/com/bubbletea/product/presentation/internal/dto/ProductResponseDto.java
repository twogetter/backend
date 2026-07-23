package com.bubbletea.product.presentation.internal.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema
@Builder
public record ProductResponseDto(
    Long pid,
    String name,
    Long price,
    String productStatus,
    Long artistId
) {

}
