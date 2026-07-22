package com.bubbletea.payment.scheduler;

import com.bubbletea.payment.processor.PaymentOutboxProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {

    private final PaymentOutboxProcessor paymentOutboxProcessor;

    @Scheduled(fixedDelay = 500)
    public void processOutboxEvents() {
        paymentOutboxProcessor.postEvent();
    }

    @Scheduled(fixedDelay = 300000)
    public void cleanupStaleOutboxEvents() {
        paymentOutboxProcessor.cleanEvent();
    }
}
