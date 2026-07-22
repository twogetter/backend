package com.bubbletea.order.presentation.external.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구독 주문 접수 응답")
public record OrderResponseDto(
    @Schema(description = "주문 ID", example = "1")
    Long orderId,

    @Schema(description = "구독 ID", example = "1")
    Long subscriptionId,

    @Schema(description = "주문 상태(접수 시 PENDING)", example = "PENDING")
    String status
) {
  public static OrderResponseDto of(Long orderId, Long subscriptionId, String status) {
    return new OrderResponseDto(orderId, subscriptionId, status);
  }
}
