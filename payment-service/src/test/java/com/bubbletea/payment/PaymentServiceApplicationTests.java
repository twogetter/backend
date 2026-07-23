package com.bubbletea.payment;

import com.bubbletea.commontest.container.KafkaTestContainer;
import com.bubbletea.commontest.container.PostgresTestContainer;
import com.bubbletea.payment.scheduler.PaymentOutboxScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentServiceApplicationTests implements PostgresTestContainer, KafkaTestContainer {

    @Test
    void contextLoads() {
    }

}
