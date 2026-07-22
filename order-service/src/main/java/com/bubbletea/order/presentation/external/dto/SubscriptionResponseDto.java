package com.bubbletea.order.presentation.external.dto;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "회원 구독 정보/상태 조회 응답")
public record SubscriptionResponseDto(
    @Schema(description = "구독 ID", example = "1")
    Long subscriptionId,

    @Schema(description = "상품(구독권) ID", example = "1")
    Long productId,

    @Schema(description = "상품명", example = "아티스트 일반 구독권")
    String productName,

    @Schema(description = "구독 상태", example = "ACTIVE",
        allowableValues = {"PENDING", "ACTIVE", "PAUSED"})
    String status,

    @Schema(description = "결제 금액", example = "4900")
    BigDecimal amount,

    @Schema(description = "다음 결제 예정일", example = "2026-08-21")
    LocalDate nextBillingDate,

    @Schema(description = "구독 시작일시")
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
