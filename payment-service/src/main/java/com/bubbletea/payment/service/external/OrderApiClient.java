package com.bubbletea.payment.service.external;

import com.bubbletea.payment.config.OrderFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderApiClient {

//    private static final String ORDER_SERVICE_BASE_URL = "https://api.tosspayments.com/v1";

    private final OrderFeignClient orderFeignClient;

    public Long getOrderAmount(Long orderId) {
        return orderFeignClient.getOrderAmount(orderId).userId();
    }

}
