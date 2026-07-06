package com.bubbletea.order.prresentation.internal;

import com.bubbletea.order.prresentation.internal.dto.OrderAmountResponseDto;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderInternalController {

  @GetMapping("/{orderId}/amount")
  public ResponseEntity<OrderAmountResponseDto> verifyOrderAmount(
      @PathVariable("orderId") Long orderId) {

    OrderAmountResponseDto response = OrderAmountResponseDto.of(100L, orderId,
        BigDecimal.valueOf(9900));
    return ResponseEntity.ok(response);
  }
}
