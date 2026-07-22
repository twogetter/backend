package com.bubbletea.product.presentation.controller;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.product.application.product.ProductQueryService;
import com.bubbletea.product.application.product.ProductRegistrationService;
import com.bubbletea.product.application.product.dto.ProductDetailResponseDto;
import com.bubbletea.product.application.product.dto.ProductListResponseDto;
import com.bubbletea.product.application.product.dto.ProductResponseDto;
import com.bubbletea.product.presentation.dto.ProductRegisterRequestDto;
import com.bubbletea.product.presentation.swagger.ProductControllerDocs;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController implements ProductControllerDocs {

    private final ProductRegistrationService productRegistrationService;
    private final ProductQueryService productQueryService;

    @Override
    @PostMapping
    public ApiResponse<ProductResponseDto> registerProduct(
        @Valid @RequestBody ProductRegisterRequestDto request
    ) {
        ProductResponseDto response = productRegistrationService.register(request);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping
    public ApiResponse<ProductListResponseDto> getProducts(
        @RequestParam(required = false) String groupName,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") @Min(1) @Max(30) int size
    ) {
        return ApiResponse.success(
            productQueryService.getProductList(groupName, cursor, size));
    }

    @Override
    @GetMapping("/groups")
    public ApiResponse<List<String>> getFilterableGroups() {
        return ApiResponse.success(productQueryService.getFilterableGroupNames());
    }

    @Override
    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponseDto> getProduct(
        @PathVariable String productId
    ) {
        return ApiResponse.success(productQueryService.getProductDetail(productId));
    }

    @Override
    @GetMapping("/search")
    public ApiResponse<ProductListResponseDto> searchProducts(
        @RequestParam String keyword,
        @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") @Min(1) @Max(30) int size
    ) {
        return ApiResponse.success(productQueryService.searchProducts(keyword, cursor, size));
    }

}
