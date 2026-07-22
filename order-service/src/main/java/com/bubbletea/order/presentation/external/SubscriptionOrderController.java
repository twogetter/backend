package com.bubbletea.order.presentation.external;

import com.bubbletea.common.response.ApiResponse;
import com.bubbletea.order.application.SubscriptionOrderFacade;
import com.bubbletea.order.application.SubscriptionQueryService;
import com.bubbletea.order.presentation.external.dto.OrderResponseDto;
import com.bubbletea.order.presentation.external.dto.SubscriptionCreateRequestDto;
import com.bubbletea.order.presentation.external.dto.SubscriptionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/subscriptions")
@RequiredArgsConstructor
@Validated
@Tag(name = "Subscription Order", description = "구독 주문 생성·조회 API")
public class SubscriptionOrderController {
  private final SubscriptionOrderFacade subscriptionOrderFacade;
  private final SubscriptionQueryService subscriptionQueryService;

  @PostMapping
  @Operation(summary = "구독 주문 생성(비동기)",
      description = "주문을 접수(202/PENDING)하고 결제는 비동기로 처리된다. 최종 결과는 구독 조회로 확인한다. "
          + "같은 Idempotency-Key 재요청은 새 주문을 만들지 않고 기존 주문을 반환한다.")
  public ResponseEntity<ApiResponse<OrderResponseDto>> createSubscription(
      @Parameter(description = "게이트웨이가 주입하는 회원 ID", required = true, example = "1")
      @RequestHeader("X-User-Id") Long memberId,
      @Parameter(description = "멱등 키(재시도 시 동일 값 재사용, 빈 값 불가)", required = true,
          example = "9b2c1e2a-4f6d-4a1b-8c3e-1f2a3b4c5d6e")
      @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
      @Valid @RequestBody SubscriptionCreateRequestDto request) {

    OrderResponseDto response =
        subscriptionOrderFacade.createSubscription(memberId, idempotencyKey, request);
    // 비동기 처리: 주문을 접수(PENDING)하고 결과는 이후 결제 결과 이벤트로 확정된다.
    return ResponseEntity
        .status(HttpStatus.ACCEPTED)
        .body(ApiResponse.success(response, "구독 주문이 접수되었습니다. 결제 처리 중입니다."));
  }

  @GetMapping
  @Operation(summary = "내 구독 목록 조회", description = "회원의 구독(취소분 제외)을 최신순으로 조회한다.")
  public ResponseEntity<ApiResponse<List<SubscriptionResponseDto>>> getMySubscriptions(
      @Parameter(description = "게이트웨이가 주입하는 회원 ID", required = true, example = "1")
      @RequestHeader("X-User-Id") Long memberId) {
    return ResponseEntity.ok(
        ApiResponse.success(subscriptionQueryService.getMemberSubscriptions(memberId)));
  }

  @GetMapping("/{subscriptionId}")
  @Operation(summary = "단건 구독 상태 조회",
      description = "비동기 생성 결과 폴링용. PENDING(처리 중) → ACTIVE(성공), 404 = 실패/취소.")
  public ResponseEntity<ApiResponse<SubscriptionResponseDto>> getSubscription(
      @Parameter(description = "게이트웨이가 주입하는 회원 ID", required = true, example = "1")
      @RequestHeader("X-User-Id") Long memberId,
      @Parameter(description = "구독 ID", example = "1")
      @PathVariable Long subscriptionId) {
    return ResponseEntity.ok(
        ApiResponse.success(subscriptionQueryService.getSubscription(memberId, subscriptionId)));
  }
}
