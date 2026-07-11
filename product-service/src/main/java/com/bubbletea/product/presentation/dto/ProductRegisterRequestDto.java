package com.bubbletea.product.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;


public record ProductRegisterRequestDto(
    @NotBlank
    String artistId,

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
