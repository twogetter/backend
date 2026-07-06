package com.bubbletea.product.presentation.internal;


import com.bubbletea.product.presentation.internal.dto.ProductResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductInternalController {

    @GetMapping("/{productId}/validation")
    public ProductResponseDto getProduct(@PathVariable Long productId) {
        return ProductResponseDto.builder()
            .price(5900L)
            .productStatus("ACTIVE")
            .artistId(1L)
            .build();
    }

}

