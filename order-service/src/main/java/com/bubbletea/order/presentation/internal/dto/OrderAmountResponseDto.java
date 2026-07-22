package com.bubbletea.order.presentation.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "주문 금액 검증 응답(internal)")
public record OrderAmountResponseDto(
    @Schema(description = "회원 ID", example = "1")
    Long memberId,

    @Schema(description = "주문 ID", example = "1")
    Long orderId,

    @Schema(description = "주문 결제 금액(권위값)", example = "4900")
    BigDecimal totalAmount
) {
  public static OrderAmountResponseDto of(Long memberId, Long orderId, BigDecimal totalAmount){
    return new OrderAmountResponseDto(memberId,orderId,totalAmount);
  }
}
