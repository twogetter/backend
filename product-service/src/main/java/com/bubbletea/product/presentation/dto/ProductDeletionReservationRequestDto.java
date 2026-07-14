package com.bubbletea.product.presentation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ProductDeletionReservationRequestDto(

    @NotNull
    LocalDateTime deleteDate
) {

}
