package com.bubbletea.product.presentation.controller;


import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.product.application.reservation.ProductReservationQueryService;
import com.bubbletea.product.application.reservation.ProductReservationService;
import com.bubbletea.product.application.reservation.dto.ReservationDetailResponseDto;
import com.bubbletea.product.application.reservation.dto.ReservationListResponseDto;
import com.bubbletea.product.domain.reservation.ReservationCategory;
import com.bubbletea.product.domain.reservation.ReservationStatus;
import com.bubbletea.product.presentation.dto.ProductDeletionReservationRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductReservationController {

    private final ProductReservationQueryService reservationQueryService;
    private final ProductReservationService productReservationService;

    @GetMapping("/reservations")
    public ApiResponse<ReservationListResponseDto> getReservations(
        @RequestParam(required = false) ReservationCategory category,
        @RequestParam(required = false) ReservationStatus status,
        @PageableDefault(size = 20, sort = "scheduledAt", direction = Sort.Direction.ASC)
        Pageable pageable
    ) {
        return ApiResponse.success(
            reservationQueryService.getReservations(category, status, pageable));
    }

    @GetMapping("/reservations/{reservationId}")
    public ApiResponse<ReservationDetailResponseDto> getReservation(
        @PathVariable String reservationId
    ) {
        return ApiResponse.success(reservationQueryService.getReservation(reservationId));
    }

    @PostMapping("/{productId}/deletion-reservations")
    public ApiResponse<Void> reserveDeletion(
        @PathVariable String productId,
        @Valid @RequestBody ProductDeletionReservationRequestDto request
    ) {
        productReservationService.reserveDeletion(productId, request);
        return ApiResponse.success(null);
    }

}
