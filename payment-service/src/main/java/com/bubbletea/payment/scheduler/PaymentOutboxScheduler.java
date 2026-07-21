package com.bubbletea.payment.scheduler;

import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import com.bubbletea.payment.processor.OutboxEventProcessor;
import com.bubbletea.payment.processor.PaymentOutboxProcessor;
import com.bubbletea.payment.repository.PaymentOutboxRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.transaction.support.TransactionTemplate;

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
