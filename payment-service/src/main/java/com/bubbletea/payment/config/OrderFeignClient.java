package com.bubbletea.payment.config;

import com.bubbletea.payment.service.dto.BrandpayAuthDetailResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "orderFeignClient", url = "${feign.order-service.url:order-service:8400}")
public interface OrderFeignClient {

    @GetMapping("/orders/{orderId}/amount")
    BrandpayAuthDetailResponseDto getOrderAmount(@PathVariable("orderId") Long orderId);
    // DTO 만들어야 함
}
