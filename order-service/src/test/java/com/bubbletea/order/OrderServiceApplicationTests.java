package com.bubbletea.order;

import com.bubbletea.order.support.OrderIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// spring.config.name=order-it → 메인 application.yaml(configserver import) 대신 order-it.yml 로드.
// OrderIntegrationTestSupport 가 Postgres·Kafka(Testcontainers) + 디스커버리(WireMock)를 제공한다.
@SpringBootTest(properties = "spring.config.name=order-it")
class OrderServiceApplicationTests extends OrderIntegrationTestSupport {

    @Test
    void contextLoads() {
    }

}
