package com.bubbletea.order.infrastructure.outbox;

import com.bubbletea.order.domain.entity.OutboxEvent;
import com.bubbletea.order.domain.enums.OutboxStatus;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PENDING 아웃박스 레코드를 주기적으로 폴링하여 Kafka로 발행하는 릴레이.
 * 발행 성공을 확인한 뒤 PUBLISHED로 전이하며, 실패 건은 다음 주기에 재시도한다(at-least-once).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

  private static final String HEADER_DOMAIN = "domainName";
  private static final String HEADER_EVENT_TYPE = "eventType";
  private static final String DOMAIN_NAME = "order";

  private final BillingScheduleRepository.OutboxEventRepository outboxEventRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Scheduled(fixedDelayString = "${order.outbox.relay-delay-ms:5000}")
  @Transactional
  public void publishPending() {
    List<OutboxEvent> events =
        outboxEventRepository.findTop100ByStatusOrderByIdAsc(OutboxStatus.PENDING);
    if (events.isEmpty()) {
      return;
    }

    for (OutboxEvent event : events) {
      try {
        // 저장된 JSON 문자열을 다시 객체로 파싱해 JacksonJsonSerializer가 유효한 JSON으로 재직렬화
        Object payload = objectMapper.readValue(event.getPayload(), Object.class);
        Message<Object> message = MessageBuilder
            .withPayload(payload)
            .setHeader(KafkaHeaders.TOPIC, event.getTopic())
            .setHeader(KafkaHeaders.KEY, event.getMessageKey())
            .setHeader(HEADER_DOMAIN, DOMAIN_NAME)
            .setHeader(HEADER_EVENT_TYPE, event.getEventType())
            .build();

        kafkaTemplate.send(message).get(); // 발행 확인 후 상태 전이
        event.markPublished();

        log.info("[Outbox] 이벤트 발행 완료. id={}, type={}, topic={}",
            event.getId(), event.getEventType(), event.getTopic());
      } catch (Exception e) {
        log.error("[Outbox] 이벤트 발행 실패(다음 주기 재시도). id={}, error={}",
            event.getId(), e.getMessage(), e);
        // status 를 PENDING 으로 유지하여 다음 스케줄에 재처리
      }
    }
  }
}
