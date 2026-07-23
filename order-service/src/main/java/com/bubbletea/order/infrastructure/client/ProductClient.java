package com.bubbletea.order.infrastructure.client;

import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {
  // product-service 실제 내부 검증 API 경로에 정렬.
  @GetMapping("/api/v1/products/{productId}/validation")
  ProductInfoResponseDto getProductInfo(@PathVariable("productId") Long productId);
}
