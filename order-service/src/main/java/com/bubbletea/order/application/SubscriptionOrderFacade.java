package com.bubbletea.order.application;

import com.bubbletea.order.domain.dto.CreatedOrderContextDto;
import com.bubbletea.order.domain.entity.IdempotencyKey;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.repository.IdempotencyKeyRepository;
import com.bubbletea.order.infrastructure.client.MemberClient;
import com.bubbletea.order.infrastructure.client.ProductClient;
import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import com.bubbletea.order.presentation.external.dto.OrderResponseDto;
import com.bubbletea.order.presentation.external.dto.SubscriptionCreateRequestDto;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionOrderFacade {

  private final MemberClient memberClient;
  private final ProductClient productClient;
  private final OrderCreationService orderCreationService;
  private final IdempotencyKeyRepository idempotencyKeyRepository;

  public OrderResponseDto createSubscription(Long memberId, String idempotencyKey,
      SubscriptionCreateRequestDto request) {
    // 멱등 fast-path: 같은 (회원, 키) 요청이면 검증·생성 없이 기존 결과 반환.
    Optional<IdempotencyKey> existing =
        idempotencyKeyRepository.findByMemberIdAndIdempotencyKey(memberId, idempotencyKey);
    if (existing.isPresent()) {
      return toResponse(existing.get());
    }

    // 0. 외부 검증/조회 (동기)
    memberClient.validateMember(memberId);
    ProductInfoResponseDto product = productClient.getProductInfo(request.productId());

    try {
      // 1. 주문 PENDING 생성 + 멱등키 저장 + 결제요청 아웃박스 기록(한 트랜잭션)
      CreatedOrderContextDto ctx = orderCreationService.createPendingOrder(
          memberId, product, request.paymentMethodId(), idempotencyKey);
      // 2. 접수 응답(PENDING). 결과는 PaymentResultEvent 수신 후 BillingResultService가 확정.
      return OrderResponseDto.of(ctx.orderId(), ctx.subscriptionId(), OrderStatus.PENDING.name());
    } catch (DataIntegrityViolationException e) {
      // 동시 중복 요청이 먼저 커밋됨(유니크 제약 위반) → 기존 결과 반환.
      return idempotencyKeyRepository.findByMemberIdAndIdempotencyKey(memberId, idempotencyKey)
          .map(this::toResponse)
          .orElseThrow(() -> e);
    }
  }

  private OrderResponseDto toResponse(IdempotencyKey key) {
    return OrderResponseDto.of(key.getOrderId(), key.getSubscriptionId(), OrderStatus.PENDING.name());
  }
}
