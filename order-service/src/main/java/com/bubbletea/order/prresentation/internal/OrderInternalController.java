package com.bubbletea.order.prresentation.internal;

import com.bubbletea.order.infrastructure.kafka.OrderEventProducer;
import com.bubbletea.order.infrastructure.kafka.dto.SubscriptionRenewalEvent;
import com.bubbletea.order.prresentation.internal.dto.OrderAmountResponseDto;
import java.math.BigDecimal;
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
public class OrderInternalController {

  @GetMapping("/{orderId}/amount")
  public ResponseEntity<OrderAmountResponseDto> verifyOrderAmount(
      @PathVariable("orderId") Long orderId) {

    OrderAmountResponseDto response = OrderAmountResponseDto.of(100L, orderId, BigDecimal.valueOf(4900));
    return ResponseEntity.ok(response);
  }


  //-------- Kafka Mocking Testing --------------

  private final OrderEventProducer orderEventProducer;

  @PostMapping("/mock-renewal-event")
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
