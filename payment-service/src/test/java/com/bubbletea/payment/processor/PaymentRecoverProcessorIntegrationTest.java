package com.bubbletea.payment.processor;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.bubbletea.commontest.container.KafkaTestContainer;
import com.bubbletea.commontest.container.PostgresTestContainer;
import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.entity.enums.CancelStatus;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.repository.PaymentCancelRepository;
import com.bubbletea.payment.repository.PaymentHistoryRepository;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.PaymentOutboxRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.repository.UserBrandpayAuthRepository;
import com.bubbletea.payment.support.fixture.PaymentCancelFixture;
import com.bubbletea.payment.support.helper.BillingTestSetupHelper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PaymentRecoverProcessorIntegrationTest implements PostgresTestContainer, KafkaTestContainer {

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
    private PaymentRecoverProcessor paymentRecoverProcessor;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentCancelRepository paymentCancelRepository;
    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;
    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    @Autowired
    private UserBrandpayAuthRepository userBrandpayAuthRepository;
    @Autowired
    private BillingTestSetupHelper setupHelper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        paymentOutboxRepository.deleteAll();
        paymentHistoryRepository.deleteAll();
        paymentCancelRepository.deleteAll();
        paymentRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        userBrandpayAuthRepository.deleteAll();
        WIREMOCK.resetToDefaultMappings();
        WIREMOCK.resetRequests();
    }

    @Test
    @DisplayName("UNKNOWN_HOLD 결제는 토스 상태가 DONE이면 정상 결제로 복구된다")
    void recoverAllHoldCompletesPayment() {
        // given
        Payment payment = createHoldPayment(
                400L,
                "toss-order-500",
                "recover-paykey-500",
                PaymentStatus.UNKNOWN_HOLD,
                BigDecimal.valueOf(10000)
        );
        WIREMOCK.stubFor(get(urlPathEqualTo("/payments/recover-paykey-500"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBody("""
                                {
                                  "paymentKey": "recover-paykey-500",
                                  "orderId": "toss-order-500",
                                  "status": "DONE",
                                  "totalAmount": 10000,
                                  "method": "BRANDPAY"
                                }
                                """)));

        // when
        paymentRecoverProcessor.recoverSinglePayment(paymentRepository.findById(payment.getId()).orElseThrow());

        // then
        Payment persisted = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paymentHistoryRepository.count()).isEqualTo(1);
        List<PaymentOutbox> outboxes = paymentOutboxRepository.findAll();
        assertThat(outboxes)
                .hasSize(2)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);

        WIREMOCK.verify(getRequestedFor(urlEqualTo("/payments/recover-paykey-500"))
                .withHeader("Authorization", equalTo("Basic dGVzdC1hcGktc2VjcmV0Og==")));
    }

    @Test
    @DisplayName("UNKNOWN_HOLD 결제는 토스 상태가 FAILED이면 실패 결제로 확정된다")
    void recoverAllHoldFailsPaymentWhenTossReturnsFailed() {
        // given
        Payment payment = createHoldPayment(
                401L,
                "toss-order-501",
                "recover-paykey-501",
                PaymentStatus.UNKNOWN_HOLD,
                BigDecimal.valueOf(11000)
        );

        WIREMOCK.stubFor(get(urlPathEqualTo("/payments/recover-paykey-501"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBody("""
                                {
                                  "paymentKey": "recover-paykey-501",
                                  "orderId": "toss-order-501",
                                  "status": "FAILED",
                                  "totalAmount": 11000,
                                  "method": "BRANDPAY"
                                }
                                """)));

        // when
        paymentRecoverProcessor.recoverSinglePayment(paymentRepository.findById(payment.getId()).orElseThrow());

        // then
        Payment persisted = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(paymentHistoryRepository.count()).isEqualTo(1);
        assertThat(paymentOutboxRepository.findAll())
                .hasSize(2)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);

        WIREMOCK.verify(getRequestedFor(urlEqualTo("/payments/recover-paykey-501")));
    }

    @Test
    @DisplayName("UNKNOWN_HOLD 취소는 토스 취소 성공 후 결제 취소 상태로 복구된다")
    void recoverAllHoldCompletesCancel() {
        // given
        Payment payment = createHoldPayment(
                402L,
                "toss-order-502",
                "recover-paykey-502",
                PaymentStatus.CANCEL_UNKNOWN_HOLD,
                BigDecimal.valueOf(12000)
        );

        PaymentCancel cancel = paymentCancelRepository.save(
                PaymentCancelFixture.create(payment)
                        .cancelAmount(BigDecimal.valueOf(12000))
                        .build()
        );

        markOlder(payment.getId(), cancel.getId());

        WIREMOCK.stubFor(post(urlPathEqualTo("/payments/recover-paykey-502/cancel"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBody("""
                                {
                                  "paymentKey": "recover-paykey-502",
                                  "status": "CANCELLED",
                                  "cancels": [
                                    {
                                      "cancellationKey": "cxl-1",
                                      "cancelAmount": 12000,
                                      "cancelReason": "사용자 요청",
                                      "canceledAt": "2026-07-21T18:00:00"
                                    }
                                  ]
                                }
                                """)));

        // when
        paymentRecoverProcessor.recoverAllHold();

        // then
        Payment persisted = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(persisted.getRefundableAmount()).isEqualByComparingTo("0.0000");
        assertThat(paymentCancelRepository.findById(cancel.getId()).orElseThrow().getStatus())
                .isEqualTo(CancelStatus.SUCCESS);
        assertThat(paymentHistoryRepository.count()).isEqualTo(1);
        assertThat(paymentOutboxRepository.findAll())
                .hasSize(2)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);

        WIREMOCK.verify(postRequestedFor(urlPathEqualTo("/payments/recover-paykey-502/cancel")));
    }

    private Payment createHoldPayment(
            Long userId,
            String tossOrderId,
            String paymentKey,
            PaymentStatus status,
            BigDecimal amount
    ) {
        UserBrandpayAuth auth = setupHelper.saveAuth(builder -> builder.userId(userId));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);
        Payment payment = setupHelper.savePayment(userId, tossOrderId, amount.longValue(), method, builder -> {
            builder.paymentKey(paymentKey);
            builder.refundableAmount(amount);
            builder.status(status);
        });
        return paymentRepository.save(payment);
    }

    private void markOlder(Long paymentId, Long paymentCancelId) {
        LocalDateTime oldTime = LocalDateTime.now().minusMinutes(10);
        Timestamp timestamp = Timestamp.valueOf(oldTime);
        jdbcTemplate.update("update payments set created_at = ?, updated_at = ? where id = ?", timestamp, timestamp, paymentId);
        if (paymentCancelId != null) {
            jdbcTemplate.update("update payment_cancels set created_at = ?, updated_at = ? where id = ?",
                    timestamp, timestamp, paymentCancelId);
        }
    }
}
