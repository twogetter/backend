package com.bubbletea.order.infrastructure.outbox;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.order.domain.entity.OutboxEvent;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRecorder {

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public void record(String aggregateType, Long aggregateId, String eventType,
      String topic, String messageKey, Object payload) {
    record(aggregateType, aggregateId, eventType, topic, messageKey, null, payload);
  }

  /** 추가 Kafka 헤더를 함께 기록한다(예: {@code X-User-Id}). */
  public void record(String aggregateType, Long aggregateId, String eventType,
      String topic, String messageKey, Map<String, String> headers, Object payload) {
    outboxEventRepository.save(OutboxEvent.builder()
        .aggregateType(aggregateType)
        .aggregateId(String.valueOf(aggregateId))
        .eventType(eventType)
        .topic(topic)
        .messageKey(messageKey)
        .headers(headers == null ? null : serialize(headers))
        .payload(serialize(payload))
        .build());
  }

  private String serialize(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new AppException(OrderErrorCode.OUTBOX_SERIALIZATION_FAILED);
    }
  }
}
