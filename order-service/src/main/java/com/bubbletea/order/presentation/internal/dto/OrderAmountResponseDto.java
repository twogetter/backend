package com.bubbletea.order.presentation.internal.dto;

import java.math.BigDecimal;

public record OrderAmountResponseDto(
    Long memberId,
    Long orderId,
    BigDecimal totalAmount
) {
  public static OrderAmountResponseDto of(Long memberId, Long orderId, BigDecimal totalAmount){
    return new OrderAmountResponseDto(memberId,orderId,totalAmount);
  }
}
