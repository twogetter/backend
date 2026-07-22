package com.bubbletea.order.presentation.external;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.order.application.SubscriptionOrderFacade;
import com.bubbletea.order.presentation.external.dto.OrderResponseDto;
import com.bubbletea.order.presentation.external.dto.SubscriptionCreateRequestDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/subscriptions")
@RequiredArgsConstructor
@Validated
public class SubscriptionOrderController {
  private final SubscriptionOrderFacade subscriptionOrderFacade;

  @PostMapping
  public ResponseEntity<ApiResponse<OrderResponseDto>> createSubscription(
      @RequestHeader("X-User-Id") Long memberId,
      @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
      @Valid @RequestBody SubscriptionCreateRequestDto request) {

    OrderResponseDto response =
        subscriptionOrderFacade.createSubscription(memberId, idempotencyKey, request);
    // 비동기 처리: 주문을 접수(PENDING)하고 결과는 이후 결제 결과 이벤트로 확정된다.
    return ResponseEntity
        .status(HttpStatus.ACCEPTED)
        .body(ApiResponse.success(response, "구독 주문이 접수되었습니다. 결제 처리 중입니다."));
  }
}
