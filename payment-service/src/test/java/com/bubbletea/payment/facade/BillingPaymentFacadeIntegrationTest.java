package com.bubbletea.payment.facade;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bubbletea.commontest.container.PostgresTestContainer;
import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.exception.PaymentTossApiException;
import com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent;
import com.bubbletea.payment.repository.PaymentHistoryRepository;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.PaymentOutboxRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.repository.UserBrandpayAuthRepository;
import com.bubbletea.payment.support.helper.BillingTestSetupHelper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class BillingPaymentFacadeIntegrationTest implements PostgresTestContainer {

    private static final WireMockServer WIREMOCK = new WireMockServer(
            WireMockConfiguration.wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderDirectory("src/test/resources")
                    .globalTemplating(true)
    );

    static {
        WIREMOCK.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("toss.payments.base-url", WIREMOCK::baseUrl);
    }

    @AfterAll
    static void tearDown() {
        WIREMOCK.stop();
    }

    @Autowired
    private BillingPaymentFacade billingPaymentFacade;
    @Autowired
    private UserBrandpayAuthRepository userBrandpayAuthRepository;
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;
    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;
    @Autowired
    private BillingTestSetupHelper setupHelper;

    @BeforeEach
    void setUp() {
        paymentOutboxRepository.deleteAll();
        paymentHistoryRepository.deleteAll();
        paymentRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        userBrandpayAuthRepository.deleteAll();
        WIREMOCK.resetAll();
    }

    @Test
    @DisplayName("정기 결제 승인 성공 시 결제가 완료되고 아웃박스 이벤트가 적재된다")
    void executeBillingSuccess() {
        // given
        UserBrandpayAuth auth = setupHelper.saveAuth(builder -> builder.userId(21L));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);
        BillingEvent event = setupHelper.createDefaultBillingEvent(method);

        // when
        billingPaymentFacade.executeBilling(21L, 100L, event);

        // then
        Payment payment = paymentRepository.findByOrderId(100L).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getRefundableAmount()).isEqualByComparingTo("50000.0000");
        assertThat(paymentHistoryRepository.count()).isEqualTo(1);
        List<PaymentOutbox> outboxes = paymentOutboxRepository.findAll();
        assertThat(outboxes)
                .hasSize(2)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);
        assertThat(outboxes)
                .extracting(PaymentOutbox::getTopic)
                .containsExactlyInAnyOrder(
                        "order.payment.paymentSuccess",
                        "notification.payment.paymentComplete"
                );

        WIREMOCK.verify(postRequestedFor(urlEqualTo("/brandpay/payments")));
    }

    @Test
    @DisplayName("정기 결제 승인 거절 시 결제가 실패하고 실패 이벤트가 적재된다")
    void executeBillingFailure() {
        // given
        UserBrandpayAuth auth = setupHelper.saveAuth(builder -> builder.userId(22L));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);
        BillingEvent event = setupHelper.createDefaultBillingEvent(method);

        WIREMOCK.stubFor(post(urlPathEqualTo("/brandpay/payments"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": "INVALID_CARD",
                                  "message": "카드 정보를 확인하세요."
                                }
                                """)));

        // when & then
        assertThatThrownBy(() -> billingPaymentFacade.executeBilling(22L, 100L, event))
                .isInstanceOf(PaymentTossApiException.class);

        Payment payment = paymentRepository.findByOrderId(100L).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getPaymentKey()).isNull();
        assertThat(paymentHistoryRepository.count()).isEqualTo(1);
        List<PaymentOutbox> outboxes = paymentOutboxRepository.findAll();
        assertThat(outboxes)
                .hasSize(2)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);
        assertThat(outboxes)
                .extracting(PaymentOutbox::getTopic)
                .containsExactlyInAnyOrder(
                        "order.payment.paymentFailed",
                        "notification.payment.paymentFail"
                );

        WIREMOCK.verify(postRequestedFor(urlEqualTo("/brandpay/payments")));
    }

    @Test
    @DisplayName("결제 요청 시 외부 서버가 계속 타임아웃을 뱉으면 재시도를 거쳐 결국 최종 실패한다")
    void executeBillingFailure_ContinuousTimeout() {
        // given
        UserBrandpayAuth auth = setupHelper.saveAuth(builder -> builder.userId(23L));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);
        BillingEvent event = setupHelper.createDefaultBillingEvent(method);

        WIREMOCK.stubFor(post(urlPathEqualTo("/brandpay/payments"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(504)
                        .withHeader("Content-Type", "application/json")));

        // when & then
        assertThatThrownBy(() -> billingPaymentFacade.executeBilling(23L, 100L, event))
                .isInstanceOf(PaymentTossApiException.class);

        WIREMOCK.verify(3, postRequestedFor(urlEqualTo("/brandpay/payments")));
        Payment payment = paymentRepository.findByOrderId(100L).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.UNKNOWN_HOLD);
    }
}
