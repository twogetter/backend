package com.bubbletea.product.presentation.internal;


import com.bubbletea.product.application.product.ProductQueryService;
import com.bubbletea.product.application.product.dto.ProductDetailResponseDto;
import com.bubbletea.product.presentation.internal.dto.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductInternalController implements ProductInternalControllerDocs {

    private final ProductQueryService productQueryService;

    @Override
    @GetMapping("/{id}/validation")
    public ProductResponseDto getProduct(@PathVariable Long id) {
        ProductDetailResponseDto dto = productQueryService.getProductDetailByPid(id);
        return ProductResponseDto.builder()
            .pid(dto.pid())
            .name(dto.name())
            .price(dto.price())
            .productStatus(dto.status().name())
            .artistId(dto.artistId())
            .build();
    }

}

