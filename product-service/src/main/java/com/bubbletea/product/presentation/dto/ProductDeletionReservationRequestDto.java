package com.bubbletea.product.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "상품 삭제 예약 요청")
public record ProductDeletionReservationRequestDto(

    @NotNull
    LocalDateTime deleteDate
) {

}
