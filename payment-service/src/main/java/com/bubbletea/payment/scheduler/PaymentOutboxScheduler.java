package com.bubbletea.payment.scheduler;

import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import com.bubbletea.payment.processor.OutboxEventProcessor;
import com.bubbletea.payment.repository.PaymentOutboxRepository;
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

    private final PaymentOutboxRepository outboxRepository;
    private final OutboxEventProcessor outboxEventProcessor;

    private final TransactionTemplate transactionTemplate;

    private final String processorId = UUID.randomUUID().toString();

    @Scheduled(fixedDelay = 500)
    public void processOutboxEvents() {

        List<PaymentOutbox> processingEvents = transactionTemplate.execute(status -> {
            int claimedCount = outboxRepository.claimPendingEvents(processorId);
            if (claimedCount == 0) {
                return List.of();
            }
            return outboxRepository.findByStatusAndProcessorId(OutboxStatus.PROCESSING, processorId);
        });

        if (processingEvents == null || processingEvents.isEmpty()) {
            return;
        }

        log.info("[Outbox Scheduler] 미발행 이벤트 {}건 발견. 발행을 시작합니다.", processingEvents.size());

        for (PaymentOutbox outbox : processingEvents) {
            try {
                outboxEventProcessor.sendToKafkaAndUpdateStatus(outbox);
            } catch (Exception e) {
                log.error("[Outbox Scheduler] 카프카 발행 중 에러 발생 (아웃박스 ID: {}). 다음 주기에 재시도합니다. 원인: {}",
                        outbox.getId(), e.getMessage());
                outboxRepository.rollbackMyProcessingToPending(processorId);
                break;
            }
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void cleanupStaleOutboxEvents() {

        int cleanedCount = outboxRepository.cleanupStaleProcessingEvents();

        if (cleanedCount > 0) {
            log.warn("[Outbox Cleanup] 서버 장애로 고립되었던 좀비 레코드 {}건을 PENDING으로 안전하게 복구했습니다.", cleanedCount);
        }
    }
}
