package com.bubbletea.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI notificationOpenApi() {
    return new OpenAPI()
        .info(new Info()
            .title("Notification Service API")
            .version("v1")
            .description("인앱 알림 조회, 읽음 처리, SSE 연결 API"));
  }
}
