package com.bubbletea.order.infrastructure.client;

import com.bubbletea.order.infrastructure.client.dto.PaymentRequestDto;
import com.bubbletea.order.infrastructure.client.dto.PaymentResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {
  @PostMapping("/api/v1/payments")
  PaymentResponseDto processPayment(@RequestBody PaymentRequestDto request);
}
