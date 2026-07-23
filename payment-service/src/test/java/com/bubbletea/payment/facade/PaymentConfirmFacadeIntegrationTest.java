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
import com.bubbletea.payment.repository.PaymentHistoryRepository;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.PaymentOutboxRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.repository.UserBrandpayAuthRepository;
import com.bubbletea.payment.service.dto.PaymentConfirmRequestDto;
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
class PaymentConfirmFacadeIntegrationTest implements PostgresTestContainer {

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
    private PaymentConfirmFacade paymentConfirmFacade;
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
    @DisplayName("결제 승인 성공 시 결제가 완료되고 아웃박스가 적재된다")
    void confirmSuccess() {
        // given
        UserBrandpayAuth auth = setupHelper.saveAuth(b -> b.userId(1L));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);
        Payment payment = setupHelper.saveDefaultPayment(1L, "toss-order-200", 60000L, method);
        paymentRepository.save(payment);

        // when
        paymentConfirmFacade.confirm(new PaymentConfirmRequestDto("paykey-200", "toss-order-200", 60000L, ""));

        // then
        Payment persisted = paymentRepository.findByTossOrderId("toss-order-200").orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(persisted.getPaymentKey()).isEqualTo("paykey-200");
        assertThat(paymentHistoryRepository.count()).isEqualTo(1);

        List<PaymentOutbox> outboxes = paymentOutboxRepository.findAll();
        assertThat(outboxes)
                .hasSize(2)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);
        assertThat(outboxes)
                .hasSize(2)
                .extracting(PaymentOutbox::getTopic)
                .containsExactlyInAnyOrder(
                        "order.payment.paymentSuccess",
                        "notification.payment.paymentComplete"
                );

        WIREMOCK.verify(postRequestedFor(urlEqualTo("/brandpay/payments/confirm")));
    }

    @Test
    @DisplayName("결제 승인 거절 시 결제가 실패 상태가 되고 실패 이벤트가 적재된다")
    void confirmFailure() {
        // given
        UserBrandpayAuth auth = setupHelper.saveAuth(b -> b.userId(2L));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);
        Payment payment = setupHelper.saveDefaultPayment(2L, "toss-order-201", 50000L, method);
        paymentRepository.save(payment);

        WIREMOCK.stubFor(post(urlPathEqualTo("/brandpay/payments/confirm"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\": \"INVALID_CARD\", \"message\": \"승인 거절\"}")));

        // when & then
        assertThatThrownBy(() -> paymentConfirmFacade.confirm(new PaymentConfirmRequestDto("paykey-200", "toss-order-201", 50000L, "")))
                .isInstanceOf(PaymentTossApiException.class);

        Payment persisted = paymentRepository.findByTossOrderId("toss-order-201").orElseThrow();
        // Circuit-breaker fallback converts client errors into external server error for confirmBrandpay -> hold
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(persisted.getPaymentKey()).isNull();
        assertThat(paymentHistoryRepository.count()).isEqualTo(1);

        List<PaymentOutbox> outboxes = paymentOutboxRepository.findAll();
        assertThat(outboxes)
                .hasSize(2)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);
        assertThat(outboxes)
                .hasSize(2)
                .extracting(PaymentOutbox::getTopic)
                .containsExactlyInAnyOrder(
                        "order.payment.paymentFailed",
                        "notification.payment.paymentFail"
                );

        WIREMOCK.verify(postRequestedFor(urlEqualTo("/brandpay/payments/confirm")));
    }

    @Test
    @DisplayName("결제 승인 시 외부 서버가 계속 타임아웃을 반환하면 재시도 없이 보류 상태가 된다")
    void confirmContinuousTimeout() {
        // given
        UserBrandpayAuth auth = setupHelper.saveAuth(b -> b.userId(3L));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);
        Payment payment = setupHelper.saveDefaultPayment(3L, "toss-order-202", 70000L, method);
        paymentRepository.save(payment);

        WIREMOCK.stubFor(post(urlPathEqualTo("/brandpay/payments/confirm"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(504)
                        .withHeader("Content-Type", "application/json")));

        // when & then
        assertThatThrownBy(() -> paymentConfirmFacade.confirm(new PaymentConfirmRequestDto("", "toss-order-202", 70000L, "")))
                .isInstanceOf(PaymentTossApiException.class);

        List<PaymentOutbox> outboxes = paymentOutboxRepository.findAll();
        assertThat(outboxes)
                .hasSize(2)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);
        assertThat(outboxes)
                .hasSize(2)
                .extracting(PaymentOutbox::getTopic)
                .containsExactlyInAnyOrder(
                        "order.payment.paymentHold",
                        "notification.payment.paymentHold"
                );

        WIREMOCK.verify(1, postRequestedFor(urlEqualTo("/brandpay/payments/confirm")));

        Payment persisted = paymentRepository.findByTossOrderId("toss-order-202").orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.UNKNOWN_HOLD);
    }
}
