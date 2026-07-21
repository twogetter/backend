package com.bubbletea.order.application;

import com.bubbletea.order.domain.dto.CreatedOrderContextDto;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.infrastructure.client.MemberClient;
import com.bubbletea.order.infrastructure.client.ProductClient;
import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import com.bubbletea.order.presentation.external.dto.OrderResponseDto;
import com.bubbletea.order.presentation.external.dto.SubscriptionCreateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionOrderFacade {

  private final MemberClient memberClient;
  private final ProductClient productClient;
  private final OrderCreationService orderCreationService;

  public OrderResponseDto createSubscription(Long memberId, SubscriptionCreateRequestDto request) {
    // 0. 외부 검증/조회 (동기)
    memberClient.validateMember(memberId);
    ProductInfoResponseDto product = productClient.getProductInfo(request.productId());

    // 1. 주문 PENDING 생성 + 결제요청 아웃박스 기록(한 트랜잭션)
    CreatedOrderContextDto ctx =
        orderCreationService.createPendingOrder(memberId, product, request.paymentMethodId());

    // 2. 접수 응답(PENDING). 결과는 PaymentResultEvent 수신 후 BillingResultService가 확정.
    return OrderResponseDto.of(ctx.orderId(), ctx.subscriptionId(), OrderStatus.PENDING.name());
  }
}
