package com.bubbletea.order.application;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.order.domain.dto.CreatedOrderContextDto;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import com.bubbletea.order.infrastructure.client.MemberClient;
import com.bubbletea.order.infrastructure.client.PaymentClient;
import com.bubbletea.order.infrastructure.client.ProductClient;
import com.bubbletea.order.infrastructure.client.dto.PaymentRequestDto;
import com.bubbletea.order.infrastructure.client.dto.PaymentResponseDto;
import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import com.bubbletea.order.presentation.external.dto.OrderResponseDto;
import com.bubbletea.order.presentation.external.dto.SubscriptionCreateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 신규 구독 생성 오케스트레이터.
 * 외부 호출(회원/상품/결제)은 트랜잭션 밖에서 수행하고, DB 쓰기는
 * 주문 생성(TX1) · 결제 결과 반영(TX2) 두 개의 트랜잭션으로 분리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionOrderFacade {

  private static final String PAYMENT_NO_RESPONSE = "결제 서비스 응답 없음";
  private static final String PAYMENT_CALL_FAILED = "결제 호출 실패";

  private final MemberClient memberClient;
  private final ProductClient productClient;
  private final PaymentClient paymentClient;
  private final OrderCreationService orderCreationService;
  private final PaymentResultService paymentResultService;

  public OrderResponseDto createSubscription(Long memberId, SubscriptionCreateRequestDto request) {
    // 0. 외부 검증/조회
    memberClient.validateMember(memberId);
    ProductInfoResponseDto product = productClient.getProductInfo(request.productId());

    // 1. TX1 - 주문 생성 (PENDING 상태로 커밋)
    CreatedOrderContextDto ctx =
        orderCreationService.createPendingOrder(memberId, product, request.paymentMethodId());

    // 2. 결제 요청 (트랜잭션 밖 외부 호출)
    boolean success;
    String failReason;
    try {
      PaymentResponseDto payment = paymentClient.processPayment(
          new PaymentRequestDto(ctx.orderId(), memberId, ctx.amount(), request.paymentMethodId()));
      success = payment != null && payment.success();
      failReason = (payment == null) ? PAYMENT_NO_RESPONSE : payment.failReason();
    } catch (Exception e) {
      // Feign 네트워크/HTTP(타임아웃·5xx 등) 예외를 실패로 변환해 TX2 보상(소프트 삭제 + PaymentFailed 아웃박스)이 실행되게 한다.
      // 타임아웃 등 모호한 실패는 실제 결제가 성공했을 수 있어 후속 정산/멱등 처리가 필요(추후 작업).
      log.warn("[Subscription] 결제 호출 예외 → 실패 처리. orderId={}", ctx.orderId(), e);
      success = false;
      failReason = PAYMENT_CALL_FAILED;
    }

    // 3. TX2 - 결제 결과 반영 (성공: 활성화+이벤트 / 실패: 이벤트 기록 후 롤백)
    OrderStatus status = paymentResultService.handlePaymentResult(ctx, success, failReason);

    if (status == OrderStatus.FAILED) {
      // TX2(롤백/아웃박스 기록) 커밋 이후 클라이언트에 실패 알림
      throw new AppException(OrderErrorCode.PAYMENT_FAILED, failReason);
    }
    return OrderResponseDto.of(ctx.orderId(), ctx.subscriptionId(), status.name());
  }
}
