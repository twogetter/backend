package com.bubbletea.order.prresentation.internal.dto;

import java.math.BigDecimal;
import lombok.Builder;
import org.springframework.core.annotation.Order;

public record OrderAmountResponseDto(
    Long memberId,
    Long orderId,
    BigDecimal totalAmount
) {
  public static OrderAmountResponseDto of(Long memberId, Long orderId, BigDecimal totalAmount){
    return new OrderAmountResponseDto(memberId,orderId,totalAmount);
  }
}
