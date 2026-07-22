package com.bubbletea.product.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Schema(description = "상품 등록 요청")
public record ProductRegisterRequestDto(
    @NotNull
    Long artistId,

    @NotBlank
    String artistName,

    String groupName,

    @NotBlank
    String description,

    @NotBlank
    String imageUrl,

    @NotNull
    @Positive
    Long price,

    @NotNull
    LocalDateTime openDate
) {

}
