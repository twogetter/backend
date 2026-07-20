package com.bubbletea.order.infrastructure.outbox;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.order.domain.entity.OutboxEvent;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRecorder {

  private final BillingScheduleRepository.OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public void record(String aggregateType, Long aggregateId, String eventType,
      String topic, String messageKey, Object payload) {
    outboxEventRepository.save(OutboxEvent.builder()
        .aggregateType(aggregateType)
        .aggregateId(String.valueOf(aggregateId))
        .eventType(eventType)
        .topic(topic)
        .messageKey(messageKey)
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
