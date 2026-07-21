package com.bubbletea.product.application.reservation.dto;

import com.bubbletea.product.domain.reservation.ProductChangeReservation;
import java.util.List;
import org.springframework.data.domain.Page;

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
