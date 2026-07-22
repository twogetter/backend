package com.bubbletea.order.presentation.external.dto;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 회원 구독 정보/상태 조회 응답. */
public record SubscriptionResponseDto(
    Long subscriptionId,
    Long productId,
    String productName,
    String status,
    BigDecimal amount,
    LocalDate nextBillingDate,
    LocalDateTime startedAt
) {

  public static SubscriptionResponseDto from(BillingSchedule schedule) {
    Subscription subscription = schedule.getSubscription();
    return new SubscriptionResponseDto(
        subscription.getId(),
        subscription.getProductId(),
        subscription.getProductName(),
        subscription.getStatus().name(),
        schedule.getAmount(),
        schedule.getNextBillingDate(),
        subscription.getStartedAt());
  }
}
