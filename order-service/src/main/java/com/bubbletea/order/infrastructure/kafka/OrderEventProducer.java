package com.bubbletea.order.infrastructure.kafka;

import com.bubbletea.order.infrastructure.kafka.dto.SubscriptionRenewalEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

  private final KafkaTemplate<String,Object> kafkaTemplate;

  private static final String TOPIC = "notifiacation.order.subscribeRenewal";

  private static final String HEADER_DOMAIN = "X-Domain";
  private static final String HEADER_EVENT_TYPE = "X-Event-Type";
  private static final String HEADER_EVENT_TIMESTAMP = "X-Event-Timestamp";

  public void sendSubscriptionRenewalEvent(SubscriptionRenewalEvent event){
    // 파티셔닝을 위한 식별 키 (동일 유저의 이벤트 순서 보장)
    String messageKey = String.valueOf(event.memberId());

    String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    // 페이로드와 커스텀 헤더를 포함한 Message 객체 조립
    Message<SubscriptionRenewalEvent> message = MessageBuilder
        .withPayload(event)
        .setHeader(KafkaHeaders.TOPIC, TOPIC)
        .setHeader(KafkaHeaders.KEY, messageKey)
        .setHeader(HEADER_DOMAIN, "order")
        .setHeader(HEADER_EVENT_TYPE, "SubscriptionRenewalImminent")
        .setHeader(HEADER_EVENT_TIMESTAMP, currentTimestamp)
        .build();

    log.info("[Kafka Producer] 알림 이벤트 발행 시도 -> Topic: {}, EventType: {}, MemberId: {}",
        TOPIC, "SubscriptionRenewalImminent", event.memberId());

    kafkaTemplate.send(message)
        .whenComplete((result, ex) -> {
          if (ex == null) {
            log.info("[Kafka Producer] 이벤트 발행 성공. Partition: {}, Offset: {}",
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          } else {
            log.error("[Kafka Producer] 이벤트 발행 실패: {}", ex.getMessage(), ex);
          }
        });
  }
}
