package com.bubbletea.payment.scheduler;

import com.bubbletea.payment.entity.Payment;
import com.bubbletea.payment.entity.PaymentCancel;
import com.bubbletea.payment.entity.enums.CancelStatus;
import com.bubbletea.payment.entity.enums.PaymentStatus;
import com.bubbletea.payment.facade.PaymentCancelFacade;
import com.bubbletea.payment.global.exception.PaymentErrorCode;
import com.bubbletea.payment.processor.PaymentRecoverProcessor;
import com.bubbletea.payment.repository.PaymentCancelRepository;
import com.bubbletea.payment.repository.PaymentRepository;
import com.bubbletea.payment.service.dto.PaymentCancelRequestDto;
import com.bubbletea.payment.service.dto.TossStatusResponseDto;
import com.bubbletea.payment.service.external.TossBrandpayApiClient;
import com.bubbletea.payment.processor.PaymentPostProcessor;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
