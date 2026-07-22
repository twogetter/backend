package com.bubbletea.order.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRenewalEvent(
    Long memberId,
    String productName,
    BigDecimal amount,
    Integer daysLeft,
    LocalDate renewalDate
) {
  public static SubscriptionRenewalEvent of(
      Long memberId,
      String productName,
      BigDecimal amount,
      Integer daysLeft,
      LocalDate renewalDate
  ) {
    return new SubscriptionRenewalEvent(
        memberId, productName, amount, daysLeft, renewalDate
    );
  }

  public static SubscriptionRenewalEvent createMock() {
    return new SubscriptionRenewalEvent(
        100L,
        "아티스트 일반 구독권",
        BigDecimal.valueOf(4900),
        3,
        LocalDate.now().plusDays(3) // 오늘 기준 3일 뒤 날짜
    );
  }
}
