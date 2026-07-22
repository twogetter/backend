package com.bubbletea.order.presentation.internal;

import com.bubbletea.order.application.OrderInternalService;
import com.bubbletea.order.domain.event.SubscriptionRenewalEvent;
import com.bubbletea.order.infrastructure.kafka.producer.OrderEventPublisher;
import com.bubbletea.order.presentation.internal.dto.OrderAmountResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Internal", description = "서비스 간 내부 호출 API")
public class OrderInternalController {

  private final OrderInternalService orderInternalService;

  @GetMapping("/{orderId}/amount")
  @Operation(summary = "주문 금액 조회(결제 금액 검증)",
      description = "payment-service가 결제 전 주문 금액을 검증하기 위해 호출하는 internal 콜백. "
          + "소프트 삭제된(취소/실패) 주문은 404.")
  public ResponseEntity<OrderAmountResponseDto> verifyOrderAmount(
      @Parameter(description = "주문 ID", required = true, example = "1")
      @PathVariable("orderId") Long orderId) {
    return ResponseEntity.ok(orderInternalService.getOrderAmount(orderId));
  }


  //-------- Kafka Mocking Testing --------------

  private final OrderEventPublisher orderEventProducer;

  @PostMapping("/mock-renewal-event")
  @Operation(summary = "[테스트] 정기결제 임박 목 이벤트 발행",
      description = "정기결제 임박 알림 Kafka 이벤트를 수동 트리거하는 테스트용 엔드포인트.")
  public ResponseEntity<Map<String, String>> triggerMockRenewalEvent() {
    log.info("[Mock API] 구독 갱신 임박 카프카 목 이벤트 발행 트리거 수신");

    // 목 데이터 생성
    SubscriptionRenewalEvent mockEvent = SubscriptionRenewalEvent.createMock();

    // 헤더와 함께 카프카로 전송
    orderEventProducer.sendSubscriptionRenewalEvent(mockEvent);

    return ResponseEntity.ok(Map.of(
        "status", "SUCCESS",
        "message", "구독 갱신 알림 목 이벤트가 성공적으로 발행되었습니다."
    ));
  }
}
