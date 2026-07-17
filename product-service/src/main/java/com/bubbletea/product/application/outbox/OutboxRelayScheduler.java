package com.bubbletea.product.application.outbox;

import com.bubbletea.product.domain.outbox.OutboxEvent;
import com.bubbletea.product.domain.outbox.OutboxEventRepository;
import com.bubbletea.product.domain.outbox.OutboxMessageSender;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxMessageSender outboxMessageSender;

    @Value("${product.outbox.stale-publishing-threshold-minutes:5}")
    private long stalePublishingThresholdMinutes;

    @Value("${product.outbox.max-retry-count:5}")
    private int maxRetryCount;

    @Scheduled(fixedDelayString = "${product.outbox.relay-fixed-delay-ms:3000}")
    public void relay() {
        recoverStalledPublishing();

        Optional<OutboxEvent> claimed;
        while ((claimed = outboxEventRepository.claimNextPending()).isPresent()) {
            publishOne(claimed.get());
        }
    }

    private void recoverStalledPublishing() {
        LocalDateTime staleBefore = LocalDateTime.now()
            .minusMinutes(stalePublishingThresholdMinutes);
        int recovered = outboxEventRepository.recoverStalledPublishing(staleBefore);
        if (recovered > 0) {
            log.warn("[outbox 복구] PUBLISHING 상태로 {}분 이상 멈춰있던 {}건을 PENDING으로 되돌림",
                stalePublishingThresholdMinutes, recovered);
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            outboxMessageSender.send(event.getTopic(), event.getPayload(), event.getHeaders());
            outboxEventRepository.markPublished(event.getId());
            log.info("[outbox 발행 성공] outboxEventId={}, topic={}", event.getId(), event.getTopic());
        } catch (Exception e) {
            handleFailure(event, e);
        }
    }

    private void handleFailure(OutboxEvent event, Exception e) {
        if (event.getRetryCount() < maxRetryCount) {
            log.warn("[outbox 발행 실패] 재시도 outboxEventId={}, topic={}, retryCount={}/{}",
                event.getId(), event.getTopic(), event.getRetryCount(), maxRetryCount, e);
            outboxEventRepository.markPendingForRetry(event.getId(), e.getMessage());
        } else {
            log.error("[outbox 발행 실패] 최대 재시도 횟수 초과 outboxEventId={}, topic={}, retryCount={}",
                event.getId(), event.getTopic(), event.getRetryCount(), e);
            outboxEventRepository.markFailed(event.getId(), e.getMessage());
        }
    }
}
