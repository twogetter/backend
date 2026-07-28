package com.bubbletea.order.infrastructure.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.bubbletea.order.domain.entity.OutboxEvent;
import com.bubbletea.order.domain.enums.OutboxStatus;
import com.bubbletea.order.domain.event.SubscriptionActivatedEvent;
import com.bubbletea.order.infrastructure.kafka.OrderKafkaTopic;
import com.bubbletea.order.support.OrderIntegrationTestSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * IT8 — 아웃박스 릴레이 발행 검증.
 *
 * <p>본 스위트({@code SubscriptionOrderIntegrationTest})는 {@code relay-delay-ms} 를 1시간으로 두어
 * 릴레이를 사실상 끄고 <b>적재(PENDING)</b> 만 검증한다. 여기서는 반대로 릴레이를 짧은 주기로 켜서
 * <b>발행과 상태 전이</b>를 검증하므로 별도 클래스로 분리한다(프로퍼티가 달라 컨텍스트도 분리된다).
 *
 * <p>{@code PUBLISHED} 전이는 {@code kafkaTemplate.send(...).get()} 이 브로커 ack 를 받은 뒤에만
 * 일어나므로, 이 전이를 확인하는 것이 곧 실제 발행을 확인하는 것이다.
 */
@SpringBootTest(properties = {
    "spring.config.name=order-it",
    "order.outbox.relay-delay-ms=500"
})
@DisplayName("IT8 아웃박스 릴레이 (relay 활성)")
class OutboxRelayIntegrationTest extends OrderIntegrationTestSupport {

  private static final long MEMBER_ID = 77L;
  private static final long PRODUCT_ID = 101L;

  @Autowired
  private OutboxRecorder outboxRecorder;
  @Autowired
  private OutboxEventRepository outboxEventRepository;

  @Test
  @DisplayName("PENDING 레코드를 Kafka 로 발행한 뒤 PUBLISHED 로 전이한다")
  void relayPublishesPendingEvents() {
    outboxRecorder.record("Subscription", 1L, "SubscriptionActivated",
        OrderKafkaTopic.SUBSCRIPTION_ACTIVATED, String.valueOf(MEMBER_ID),
        SubscriptionActivatedEvent.of(MEMBER_ID, PRODUCT_ID, LocalDateTime.now()));

    assertThat(outboxEventRepository.findTop100ByStatusOrderByIdAsc(OutboxStatus.PENDING))
        .hasSize(1);

    await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(300)).untilAsserted(() ->
        assertThat(outboxEventRepository.findTop100ByStatusOrderByIdAsc(OutboxStatus.PENDING))
            .as("릴레이가 발행 후 PENDING 을 비운다")
            .isEmpty());

    assertThat(outboxEventRepository.findAll())
        .singleElement()
        .satisfies((OutboxEvent e) -> {
          assertThat(e.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
          assertThat(e.getTopic()).isEqualTo(OrderKafkaTopic.SUBSCRIPTION_ACTIVATED);
        });
  }

  @Test
  @DisplayName("저장된 추가 헤더(X-User-Id)를 실은 레코드도 정상 발행된다")
  void relayPublishesEventWithCustomHeaders() {
    outboxRecorder.record("Subscription", 2L, "SubscriptionActivated",
        OrderKafkaTopic.SUBSCRIPTION_ACTIVATED, String.valueOf(MEMBER_ID),
        Map.of("X-User-Id", String.valueOf(MEMBER_ID)),
        SubscriptionActivatedEvent.of(MEMBER_ID, PRODUCT_ID, LocalDateTime.now()));

    await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(300)).untilAsserted(() ->
        assertThat(outboxEventRepository.findAll())
            .singleElement()
            .satisfies((OutboxEvent e) -> {
              assertThat(e.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
              assertThat(e.getHeaders()).contains("X-User-Id");
            }));
  }
}
