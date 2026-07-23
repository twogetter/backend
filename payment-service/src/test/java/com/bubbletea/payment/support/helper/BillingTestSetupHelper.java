package com.bubbletea.payment.support.helper;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentMethod;
import com.bubbletea.payment.entity.PaymentMethod.PaymentMethodBuilder;
import com.bubbletea.payment.entity.UserBrandpayAuth;
import com.bubbletea.payment.entity.UserBrandpayAuth.UserBrandpayAuthBuilder;
import com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent;
import com.bubbletea.payment.infrastructure.kafka.dto.BillingEvent.BillingEventBuilder;
import com.bubbletea.payment.repository.PaymentMethodRepository;
import com.bubbletea.payment.repository.UserBrandpayAuthRepository;
import com.bubbletea.payment.support.fixture.BillingEventFixture;
import com.bubbletea.payment.support.fixture.PaymentFixture;
import com.bubbletea.payment.support.fixture.PaymentMethodFixture;
import com.bubbletea.payment.support.fixture.UserBrandpayAuthFixture;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BillingTestSetupHelper {

    @Autowired private UserBrandpayAuthRepository authRepository;
    @Autowired private PaymentMethodRepository methodRepository;

    public UserBrandpayAuth saveDefaultAuth() {
        return authRepository.save(UserBrandpayAuthFixture.create().build());
    }

    public UserBrandpayAuth saveAuth(Consumer<UserBrandpayAuthBuilder> customizer) {
        var builder = UserBrandpayAuthFixture.create();
        customizer.accept(builder);
        return authRepository.save(builder.build());
    }

    public PaymentMethod saveDefaultMethod(UserBrandpayAuth auth) {
        return methodRepository.save(PaymentMethodFixture.create(auth).build());
    }

    public PaymentMethod saveMethod(Consumer<PaymentMethodBuilder> customizer, UserBrandpayAuth auth) {
        var builder = PaymentMethodFixture.create(auth);
        customizer.accept(builder);
        return methodRepository.save(builder.build());
    }

    public BillingEvent createDefaultBillingEvent(PaymentMethod paymentMethod) {
        return BillingEventFixture.create(paymentMethod).build();
    }

    public BillingEvent createBillingEvent(Consumer<BillingEventBuilder> customizer, PaymentMethod paymentMethod) {
        var builder = BillingEventFixture.create(paymentMethod);
        customizer.accept(builder);
        return builder.build();
    }

    public Payment saveDefaultPayment(Long userId, String tossOrderId, long amount, PaymentMethod method) {
        return PaymentFixture.create(userId, tossOrderId, amount, method).build();
    }

    public Payment savePayment(Long userId, String tossOrderId, long amount, PaymentMethod method, Consumer<Payment.PaymentBuilder> customizer) {
        var builder = PaymentFixture.create(userId, tossOrderId, amount, method);
        customizer.accept(builder);
        return builder.build();
    }
}
