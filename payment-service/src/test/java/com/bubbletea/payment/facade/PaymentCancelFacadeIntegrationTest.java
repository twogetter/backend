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
import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.entity.enums.CancelStatus;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.global.exception.PaymentSystemException;
import com.bubbletea.payment.global.exception.PaymentTossApiException;
import com.bubbletea.payment.repository.PaymentCancelRepository;
import com.bubbletea.payment.repository.PaymentHistoryRepository;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.PaymentOutboxRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.repository.UserBrandpayAuthRepository;
import com.bubbletea.payment.service.dto.PaymentCancelRequestDto;
import com.bubbletea.payment.support.helper.BillingTestSetupHelper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.math.BigDecimal;
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
class PaymentCancelFacadeIntegrationTest implements PostgresTestContainer {

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
    private PaymentCancelFacade paymentCancelFacade;
    @Autowired
    private UserBrandpayAuthRepository userBrandpayAuthRepository;
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentCancelRepository paymentCancelRepository;
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
        paymentCancelRepository.deleteAll();
        paymentRepository.deleteAll();
        paymentMethodRepository.deleteAll();
        userBrandpayAuthRepository.deleteAll();
        WIREMOCK.resetAll();
    }

    @Test
    @DisplayName("결제 취소 성공 시 결제 취소가 완료되고 아웃박스가 적재된다")
    void cancelSuccess() {
        // given
        UserBrandpayAuth auth = setupHelper.saveAuth(b -> b.userId(31L));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);

        Payment payment = setupHelper.savePayment(31L, "toss-order-300", 10000L, method, p -> {
            p.paymentKey("paykey-31");
            p.refundableAmount(BigDecimal.valueOf(10000));
            p.status(PaymentStatus.PAID);
        });

        paymentRepository.save(payment);

        PaymentCancelRequestDto dto = PaymentCancelRequestDto.builder()
                .paymentId(payment.getId())
                .cancelAmount(BigDecimal.valueOf(10000))
                .cancelReason("사용자 요청")
                .idempotencyKey(payment.getIdempotencyKey())
                .build();

        // when
        paymentCancelFacade.cancel(31L, dto, false);

        // then
        Payment persisted = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(persisted.getRefundableAmount()).isEqualByComparingTo("0.0000");
        assertThat(paymentHistoryRepository.count()).isEqualTo(1);

        List<PaymentOutbox> outboxes = paymentOutboxRepository.findAll();
        assertThat(outboxes)
                .hasSizeGreaterThanOrEqualTo(1)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);

        // ensure PaymentCancel entity was updated to SUCCESS
        PaymentCancel cancel = paymentCancelRepository.findByIdempotencyKey("payment-31-idemp").orElseThrow();
        assertThat(cancel.getStatus()).isEqualTo(CancelStatus.SUCCESS);

        WIREMOCK.verify(postRequestedFor(urlEqualTo("/payments/paykey-31/cancel")));
    }

    @Test
    @DisplayName("결제 취소 실패 시 실패 이벤트가 적재되고 상태가 실패로 변경된다")
    void cancelFailure() {
        // given
        UserBrandpayAuth auth = setupHelper.saveAuth(b -> b.userId(32L));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);

        Payment payment = setupHelper.savePayment(32L, "toss-order-301", 15000L, method, p -> {
            p.paymentKey("paykey-32");
            p.refundableAmount(BigDecimal.valueOf(15000));
            p.status(PaymentStatus.PAID);
        });

        paymentRepository.save(payment);

        WIREMOCK.stubFor(post(urlPathEqualTo("/payments/paykey-32/cancel"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"code\": \"CANCEL_NOT_ALLOWED\", \"message\": \"취소 불가\"}")));

        PaymentCancelRequestDto dto = PaymentCancelRequestDto.builder()
                .paymentId(payment.getId())
                .cancelAmount(BigDecimal.valueOf(15000))
                .cancelReason("사용자 요청")
                .idempotencyKey("cancel-301-idemp")
                .build();

        // when & then
        assertThatThrownBy(() -> paymentCancelFacade.cancel(32L, dto, false))
                .isInstanceOf(PaymentTossApiException.class);

        Payment persisted = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paymentHistoryRepository.count()).isEqualTo(1);

        List<PaymentOutbox> outboxes = paymentOutboxRepository.findAll();
        assertThat(outboxes)
                .hasSizeGreaterThanOrEqualTo(1)
                .extracting(PaymentOutbox::getStatus)
                .containsOnly(OutboxStatus.PENDING);

        PaymentCancel cancel = paymentCancelRepository.findByIdempotencyKey("cancel-301-idemp").orElseThrow();
        assertThat(cancel.getStatus()).isEqualTo(CancelStatus.FAILED);

        WIREMOCK.verify(postRequestedFor(urlEqualTo("/payments/paykey-32/cancel")));
    }

    @Test
    @DisplayName("결제 취소 시 외부 서버가 계속 타임아웃을 반환하면 보류 상태가 된다")
    void cancelContinuousTimeout() {
        // given
        UserBrandpayAuth auth = setupHelper.saveAuth(b -> b.userId(33L));
        PaymentMethod method = setupHelper.saveDefaultMethod(auth);


        Payment payment = setupHelper.savePayment(33L, "toss-order-302", 20000L, method, p -> {
            p.paymentKey("paykey-33");
            p.refundableAmount(BigDecimal.valueOf(20000));
            p.status(PaymentStatus.PAID);
        });
        paymentRepository.save(payment);

        WIREMOCK.stubFor(post(urlPathEqualTo("/payments/paykey-33/cancel"))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(504)
                        .withHeader("Content-Type", "application/json")));

        PaymentCancelRequestDto dto = PaymentCancelRequestDto.builder()
                .paymentId(payment.getId())
                .cancelAmount(BigDecimal.valueOf(5000))
                .cancelReason("부분 취소")
                .idempotencyKey("cancel-302-idemp")
                .build();

        // when & then
        assertThatThrownBy(() -> paymentCancelFacade.cancel(33L, dto, false))
                .isInstanceOf(PaymentTossApiException.class);

        Payment persisted = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.CANCEL_UNKNOWN_HOLD);

        PaymentCancel cancel = paymentCancelRepository.findByIdempotencyKey("cancel-302-idemp").orElseThrow();
        assertThat(cancel.getStatus()).isEqualTo(CancelStatus.UNKNOWN_HOLD);
    }
}
