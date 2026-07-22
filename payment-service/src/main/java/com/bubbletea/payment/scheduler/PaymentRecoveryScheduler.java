package com.bubbletea.payment.scheduler;

import com.bubbletea.payment.processor.PaymentRecoverProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRecoveryScheduler {

    private final PaymentRecoverProcessor paymentRecoverProcessor;

    @Scheduled(fixedDelay = 600000)
    public void recoverAllHoldTransactions() {
        paymentRecoverProcessor.recoverAllHold();
    }

}
