package com.bubbletea.payment.scheduler;

import com.bubbletea.payment.entity.PaymentOutbox;
import com.bubbletea.payment.entity.enums.OutboxStatus;
import com.bubbletea.payment.processor.OutboxEventProcessor;
import com.bubbletea.payment.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {

    private final PaymentOutboxRepository outboxRepository;
    private final OutboxEventProcessor outboxEventProcessor;

    @Scheduled(fixedDelay = 500)
    public void processOutboxEvents() {
        List<PaymentOutbox> pendingEvents = outboxRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("[Outbox Scheduler] 미발행 이벤트 {}건 발견. 발행을 시작합니다.", pendingEvents.size());

        for (PaymentOutbox outbox : pendingEvents) {
            try {
                // 2. 각 이벤트를 개별 트랜잭션으로 안전하게 카프카로 발행하고 상태를 변경합니다.
                outboxEventProcessor.sendToKafkaAndUpdateStatus(outbox);
            } catch (Exception e) {
                // 카프카가 다운되는 등 에러 발생 시, 루프를 중단하고 다음 스케줄러 주기 때 다시 시도합니다.
                log.error("[Outbox Scheduler] 카프카 발행 중 에러 발생 (아웃박스 ID: {}). 다음 주기에 재시도합니다. 원인: {}",
                        outbox.getId(), e.getMessage());
                break;
            }
        }
    }
}
