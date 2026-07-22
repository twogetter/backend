package com.bubbletea.product.application.reservation.dto;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "상품 예약 목록 조회 응답")
public record ReservationListResponseDto(
    List<ReservationListItemDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
    
    public static ReservationListResponseDto from(Page<ProductChangeReservation> page) {
        List<ReservationListItemDto> items = page.getContent().stream()
            .map(ReservationListItemDto::from)
            .toList();

        return new ReservationListResponseDto(
            items,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext()
        );
    }
}
