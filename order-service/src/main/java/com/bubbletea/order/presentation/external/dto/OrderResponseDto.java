package com.bubbletea.order.presentation.external.dto;

public record OrderResponseDto(
    Long orderId,
    Long subscriptionId,
    String status
) {
  public static OrderResponseDto of(Long orderId, Long subscriptionId, String status) {
    return new OrderResponseDto(orderId, subscriptionId, status);
  }
}
