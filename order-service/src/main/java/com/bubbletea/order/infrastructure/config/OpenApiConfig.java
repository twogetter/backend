package com.bubbletea.order.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI orderOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("Order Service API")
            .version("v1")
            .description("구독 주문 생성(비동기)·조회, 정기결제, 결제 금액 검증(internal) API"));
  }
}
