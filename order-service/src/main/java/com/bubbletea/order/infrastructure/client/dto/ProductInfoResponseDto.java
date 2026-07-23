package com.bubbletea.order.infrastructure.client.dto;

import java.math.BigDecimal;

/**
 * product-service 내부 검증 API(/api/v1/products/{pid}/validation)의 응답 구조에 정렬한
 * 상품(구독권) 정보. JSON 필드명이 product-service 의 ProductResponseDto 와 일치해야 한다.
 */
public record ProductInfoResponseDto(
    Long pid,
    String name,
    Long price,
    String productStatus,
    Long artistId
) {

  /** 주문 도메인에서 사용하는 상품 식별자. */
  public Long productId() {
    return pid;
  }

  /** 주문 도메인에서 사용하는 상품명. */
  public String productName() {
    return name;
  }

  /** 결제 금액(BigDecimal) 변환. */
  public BigDecimal priceAmount() {
    return price == null ? null : BigDecimal.valueOf(price);
  }
}
