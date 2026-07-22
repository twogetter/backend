package com.bubbletea.order.infrastructure.client.dto;

import java.math.BigDecimal;

/** product-service 로부터 조회한 상품(구독권) 정보. */
public record ProductInfoResponseDto(
    Long productId,
    String productName,
    BigDecimal price
) {

}
