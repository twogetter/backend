package com.bubbletea.payment.infrastructure.kafka;

import com.bubbletea.commontest.container.KafkaTestContainer;
import com.bubbletea.common.exception.AppException;
import com.bubbletea.commontest.container.PostgresTestContainer;
import com.bubbletea.payment.PaymentServiceApplication;
import com.bubbletea.payment.facade.BillingPaymentFacade;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BillingPaymentConsumerIntegrationTest implements PostgresTestContainer, KafkaTestContainer {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private BillingPaymentFacade billingPaymentFacade;

    private static final String TOPIC = "payment.order.payment-requested";

    @Test
    @DisplayName("정상 메시지 수신 시 facade 호출")
    void whenMessagePosted_thenFacadeIsCalled() {
        Long orderId = 1000L;
        Long userId = 5000L;

        BillingEvent event = BillingEvent.builder()
                .orderId(orderId)
                .methodId(1L)
                .tossOrderId("toss-1")
                .currency("KRW")
                .orderName("order")
                .totalAmount(java.math.BigDecimal.valueOf(1000))
                .build();

        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .setHeader(KafkaHeaders.KEY, String.valueOf(orderId))
                .setHeader("X-User-Id", String.valueOf(userId))
                .build();

        kafkaTemplate.send(message);
        kafkaTemplate.flush();

        verify(billingPaymentFacade, timeout(5000)).executeBilling(
                eq(userId),
                eq(orderId),
                argThat((BillingEvent e) -> e != null && orderId.equals(e.orderId()))
        );
    }

    @Test
    @DisplayName("외부서버 에러 시 카프카 재시도 발생")
    void whenFacadeThrowsExternalServerError_thenKafkaRetries() {
        Long orderId = 2000L;
        Long userId = 6000L;

        BillingEvent event = BillingEvent.builder()
                .orderId(orderId)
                .methodId(1L)
                .tossOrderId("toss-2")
                .currency("KRW")
                .orderName("order2")
                .totalAmount(java.math.BigDecimal.valueOf(2000))
                .build();

        // 첫 호출에서는 EXTERNAL_SERVER_ERROR를 던지고, 다음 호출은 정상 처리하도록 설정 -> 리트라이 확인
        doThrow(new AppException(PaymentErrorCode.EXTERNAL_SERVER_ERROR))
                .doNothing()
                .when(billingPaymentFacade).executeBilling(anyLong(), anyLong(), any(BillingEvent.class));

        var message = MessageBuilder.withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .setHeader(KafkaHeaders.KEY, String.valueOf(orderId))
                .setHeader("X-User-Id", String.valueOf(userId))
                .build();

        kafkaTemplate.send(message);
        kafkaTemplate.flush();

        // 리트라이가 발생했는지(최소 2회 호출) 확인
        verify(billingPaymentFacade, timeout(10000).atLeast(2)).executeBilling(
                eq(userId),
                eq(orderId),
                any(BillingEvent.class)
        );
    }
}
