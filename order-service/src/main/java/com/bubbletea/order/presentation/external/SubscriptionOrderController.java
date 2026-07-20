package com.bubbletea.order.presentation.external;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.order.application.SubscriptionOrderFacade;
import com.bubbletea.order.presentation.external.dto.OrderResponseDto;
import com.bubbletea.order.presentation.external.dto.SubscriptionCreateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/subscriptions")
@RequiredArgsConstructor
public class SubscriptionOrderController {
  private final SubscriptionOrderFacade subscriptionOrderFacade;

  @PostMapping
  public ResponseEntity<ApiResponse<OrderResponseDto>> createSubscription(
      @RequestHeader("X-User-Id") Long memberId,
      @Valid @RequestBody SubscriptionCreateRequestDto request) {

    OrderResponseDto response = subscriptionOrderFacade.createSubscription(memberId, request);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.success(response, "구독 주문이 생성되었습니다."));
  }
}
