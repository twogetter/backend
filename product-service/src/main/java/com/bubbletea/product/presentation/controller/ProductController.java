package com.bubbletea.product.presentation.controller;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.product.application.product.ProductRegistrationService;
import com.bubbletea.product.application.product.dto.ProductResponseDto;
import com.bubbletea.product.presentation.dto.ProductRegisterRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRegistrationService productRegistrationService;

    @PostMapping
    public ApiResponse<ProductResponseDto> registerProduct(
        @Valid @RequestBody ProductRegisterRequestDto request
    ) {
        ProductResponseDto response = productRegistrationService.register(request);
        return ApiResponse.success(response);
    }

}
