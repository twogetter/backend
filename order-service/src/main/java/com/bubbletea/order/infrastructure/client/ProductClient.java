package com.bubbletea.order.infrastructure.client;

import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {
  @GetMapping("/api/v1/products/{productId}")
  ProductInfoResponseDto getProductInfo(@PathVariable("productId") Long productId);
}
