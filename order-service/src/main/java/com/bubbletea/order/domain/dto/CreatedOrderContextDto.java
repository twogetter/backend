package com.bubbletea.order.domain.dto;

import java.math.BigDecimal;

/** TX1(주문 생성)에서 커밋된 식별자·금액을 TX2(결제 결과 반영)로 전달하는 컨텍스트. */
public record CreatedOrderContextDto(
    Long subscriptionId,
    Long scheduleId,
    Long orderId,
    Long memberId,
    Long productId,
    BigDecimal amount
) {

}
