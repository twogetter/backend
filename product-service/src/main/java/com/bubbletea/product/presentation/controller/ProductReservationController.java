package com.bubbletea.product.presentation.controller;


import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.product.application.reservation.ProductReservationService;
import com.bubbletea.product.presentation.dto.ProductDeletionReservationRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductReservationController {

    private final ProductReservationService productReservationService;

    @PostMapping("/{productId}/deletion-reservations")
    public ApiResponse<Void> reserveDeletion(
        @PathVariable String productId,
        @Valid @RequestBody ProductDeletionReservationRequestDto request
    ) {
        productReservationService.reserveDeletion(productId, request);
        return ApiResponse.success(null);
    }

}
