package com.bubbletea.order.application;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.order.domain.dto.CreatedOrderContextDto;
import com.bubbletea.order.domain.entity.IdempotencyKey;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import com.bubbletea.order.domain.repository.IdempotencyKeyRepository;
import com.bubbletea.order.domain.repository.SubscriptionRepository;
import com.bubbletea.order.infrastructure.client.MemberClient;
import com.bubbletea.order.infrastructure.client.ProductClient;
import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import com.bubbletea.order.presentation.external.dto.OrderResponseDto;
import com.bubbletea.order.presentation.external.dto.SubscriptionCreateRequestDto;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionOrderFacade {

  /**
   * 상품을 점유한 것으로 보는 구독 상태.
   *
   * <p>PAUSED(정기결제 실패로 일시정지)도 구독 보유로 취급한다 — 재결제는
   * {@link RecurringBillingService} 배치가 담당하므로 신규 주문으로 우회시키지 않는다.
   * CANCELED 는 소프트 삭제되어 조회에서 자동 제외되므로 목록에 넣지 않는다(해지 후 재구독 허용).
   */
  private static final Set<SubscriptionStatus> OCCUPYING_STATUSES = EnumSet.of(
      SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAUSED);

  private final MemberClient memberClient;
  private final ProductClient productClient;
  private final OrderCreationService orderCreationService;
  private final IdempotencyKeyRepository idempotencyKeyRepository;
  private final SubscriptionRepository subscriptionRepository;

  public OrderResponseDto createSubscription(Long memberId, String idempotencyKey,
      SubscriptionCreateRequestDto request) {
    // 멱등 fast-path: 같은 (회원, 키) 요청이면 검증·생성 없이 기존 결과 반환.
    Optional<IdempotencyKey> existing =
        idempotencyKeyRepository.findByMemberIdAndIdempotencyKey(memberId, idempotencyKey);
    if (existing.isPresent()) {
      return toResponse(existing.get());
    }

    // 중복 구독 차단(선검사) — 멱등키가 달라지는 재시도(다른 창·기기·새로고침)를 도메인 상태로 막는다.
    // 외부 호출 전에 두어 불필요한 Feign 왕복을 피한다. 완전 동시 요청은 아래 유니크 제약이 최종 차단.
    if (isOccupied(memberId, request.productId())) {
      throw new AppException(OrderErrorCode.DUPLICATE_SUBSCRIPTION);
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
      // 이 트랜잭션에는 유니크 제약이 둘 걸려 있다(멱등키, 회원+상품 중복 구독).
      // 어느 쪽이 위반됐는지는 예외 메시지 파싱 대신 재조회로 판별한다.
      return resolveConflict(memberId, product.productId(), idempotencyKey, e);
    }
  }

  /**
   * 동시 요청이 먼저 커밋되어 유니크 제약을 위반한 경우의 후처리.
   * 같은 멱등키였다면 멱등 계약대로 기존 결과를 반환하고, 같은 상품의 중복 주문이면 409로 거절한다.
   */
  private OrderResponseDto resolveConflict(Long memberId, Long productId, String idempotencyKey,
      DataIntegrityViolationException e) {
    Optional<IdempotencyKey> committed =
        idempotencyKeyRepository.findByMemberIdAndIdempotencyKey(memberId, idempotencyKey);
    if (committed.isPresent()) {
      return toResponse(committed.get());
    }
    if (isOccupied(memberId, productId)) {
      throw new AppException(OrderErrorCode.DUPLICATE_SUBSCRIPTION);
    }
    throw e;
  }

  private boolean isOccupied(Long memberId, Long productId) {
    return subscriptionRepository
        .existsByMemberIdAndProductIdAndStatusIn(memberId, productId, OCCUPYING_STATUSES);
  }

  private OrderResponseDto toResponse(IdempotencyKey key) {
    return OrderResponseDto.of(key.getOrderId(), key.getSubscriptionId(), OrderStatus.PENDING.name());
  }
}
