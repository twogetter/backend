package com.bubbletea.order.presentation;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bubbletea.order.domain.entity.OutboxEvent;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.enums.OutboxStatus;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import com.bubbletea.order.domain.event.PaymentResultEvent;
import com.bubbletea.order.domain.repository.IdempotencyKeyRepository;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import com.bubbletea.order.domain.repository.SubscriptionRepository;
import com.bubbletea.order.infrastructure.kafka.OrderKafkaTopic;
import com.bubbletea.order.infrastructure.outbox.OutboxEventRepository;
import com.bubbletea.order.support.OrderIntegrationTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// spring.config.name=order-it → 메인 application.yaml(configserver import) 대신 order-it.yml 만 로드
@SpringBootTest(properties = "spring.config.name=order-it")
@AutoConfigureMockMvc
@DisplayName("구독 주문 생성 통합 테스트 (WireMock 으로 user/product 스텁)")
class SubscriptionOrderIntegrationTest extends OrderIntegrationTestSupport {

  private static final long MEMBER_ID = 1L;
  private static final long PRODUCT_ID = 101L;
  private static final long METHOD_ID = 5L;
  private static final String URL = "/api/v1/orders/subscriptions";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private KafkaTemplate<String, Object> kafkaTemplate;

  @Autowired
  private SubscriptionRepository subscriptionRepository;
  @Autowired
  private SubscriptionOrderRepository subscriptionOrderRepository;
  @Autowired
  private IdempotencyKeyRepository idempotencyKeyRepository;
  @Autowired
  private OutboxEventRepository outboxEventRepository;

  @Test
  @DisplayName("정상 요청: 회원·상품 검증 후 주문을 PENDING 으로 접수(202)하고 결제요청 이벤트를 아웃박스에 적재한다")
  void createSubscription_success() throws Exception {
    stubMemberValid(MEMBER_ID);
    stubProduct(PRODUCT_ID, "아이유 구독권", 9900L, "ACTIVE", PRODUCT_ID);

    mockMvc.perform(post(URL)
            .header("X-User-Id", MEMBER_ID)
            .header("Idempotency-Key", "idem-success-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(PRODUCT_ID, METHOD_ID)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.status").value(OrderStatus.PENDING.name()))
        .andExpect(jsonPath("$.data.orderId").exists())
        .andExpect(jsonPath("$.data.subscriptionId").exists());

    // 외부 검증 호출이 실제로 나갔는지 확인
    WIREMOCK.verify(1, getRequestedFor(urlEqualTo("/internal/members/" + MEMBER_ID)));
    WIREMOCK.verify(1, getRequestedFor(urlEqualTo("/api/v1/products/" + PRODUCT_ID + "/validation")));

    // DB: 구독/주문 PENDING, 멱등키 1건
    assertThat(subscriptionRepository.findAll())
        .singleElement()
        .satisfies(s -> {
          assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
          assertThat(s.getMemberId()).isEqualTo(MEMBER_ID);
          assertThat(s.getProductId()).isEqualTo(PRODUCT_ID);
        });
    assertThat(subscriptionOrderRepository.findAll())
        .singleElement()
        .satisfies(o -> {
          assertThat(o.getStatus()).isEqualTo(OrderStatus.PENDING);
          assertThat(o.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(9900));
        });
    assertThat(idempotencyKeyRepository.count()).isEqualTo(1);

    // 아웃박스: 결제요청 이벤트가 PENDING 으로 적재(dual-write 방지)
    List<OutboxEvent> outbox = outboxEventRepository.findTop100ByStatusOrderByIdAsc(OutboxStatus.PENDING);
    assertThat(outbox).singleElement().satisfies(e -> {
      assertThat(e.getTopic()).isEqualTo(OrderKafkaTopic.PAYMENT_REQUESTED);
      assertThat(e.getEventType()).isEqualTo("BillingRequested");
      assertThat(e.getHeaders()).contains("X-User-Id");
    });
  }

  @Test
  @DisplayName("멱등성: 동일 Idempotency-Key 재요청은 새 구독을 만들지 않고 기존 주문을 반환한다")
  void createSubscription_idempotent() throws Exception {
    stubMemberValid(MEMBER_ID);
    stubProduct(PRODUCT_ID, "아이유 구독권", 9900L, "ACTIVE", PRODUCT_ID);
    String key = "idem-dup-1";

    MvcResult first = mockMvc.perform(post(URL)
            .header("X-User-Id", MEMBER_ID).header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content(body(PRODUCT_ID, METHOD_ID)))
        .andExpect(status().isAccepted())
        .andReturn();

    MvcResult second = mockMvc.perform(post(URL)
            .header("X-User-Id", MEMBER_ID).header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content(body(PRODUCT_ID, METHOD_ID)))
        .andExpect(status().isAccepted())
        .andReturn();

    assertThat(orderId(first)).isEqualTo(orderId(second));
    assertThat(subscriptionRepository.count()).isEqualTo(1);
    assertThat(subscriptionOrderRepository.count()).isEqualTo(1);
    // 멱등 fast-path: 두 번째 요청은 상품 검증을 다시 호출하지 않는다
    WIREMOCK.verify(1, getRequestedFor(urlEqualTo("/api/v1/products/" + PRODUCT_ID + "/validation")));
  }

  @Test
  @DisplayName("중복 구독: 멱등키가 달라도 이미 점유 중인 상품이면 409 로 거절하고 주문을 만들지 않는다")
  void createSubscription_duplicateProduct() throws Exception {
    stubMemberValid(MEMBER_ID);
    stubProduct(PRODUCT_ID, "아이유 구독권", 9900L, "ACTIVE", PRODUCT_ID);

    mockMvc.perform(post(URL)
            .header("X-User-Id", MEMBER_ID)
            .header("Idempotency-Key", "idem-dupsub-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(PRODUCT_ID, METHOD_ID)))
        .andExpect(status().isAccepted());

    // 다른 창/기기에서의 재시도를 모사 — 멱등키가 다르므로 멱등 fast-path 로는 막히지 않는다.
    mockMvc.perform(post(URL)
            .header("X-User-Id", MEMBER_ID)
            .header("Idempotency-Key", "idem-dupsub-2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(PRODUCT_ID, METHOD_ID)))
        .andExpect(status().isConflict());

    assertThat(subscriptionRepository.count()).isEqualTo(1);
    assertThat(subscriptionOrderRepository.count()).isEqualTo(1);
    // 선검사에서 걸러지므로 두 번째 요청은 멱등키를 남기지 않는다
    assertThat(idempotencyKeyRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("중복 구독: 해지(소프트 삭제)된 구독은 점유로 보지 않아 재구독이 허용된다")
  void createSubscription_allowsResubscribeAfterCancel() throws Exception {
    stubMemberValid(MEMBER_ID);
    stubProduct(PRODUCT_ID, "아이유 구독권", 9900L, "ACTIVE", PRODUCT_ID);

    MvcResult created = mockMvc.perform(post(URL)
            .header("X-User-Id", MEMBER_ID)
            .header("Idempotency-Key", "idem-resub-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(PRODUCT_ID, METHOD_ID)))
        .andExpect(status().isAccepted())
        .andReturn();

    // 신규 구독 결제 실패 → 구독/스케줄/주문이 소프트 삭제되는 보상 경로를 그대로 태운다.
    long orderId = orderId(created);
    kafkaTemplate.send(OrderKafkaTopic.PAYMENT_RESULT_FAILED, String.valueOf(orderId),
        new PaymentResultEvent(orderId, 999L, BigDecimal.valueOf(9900), "",
            PaymentResultEvent.STATUS_FAILED, "카드 한도 초과"));

    await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(500))
        .untilAsserted(() -> assertThat(subscriptionRepository.count()).isZero());

    // 취소분은 부분 유니크 인덱스(deleted_at IS NULL)에서 빠지므로 같은 상품을 다시 구독할 수 있다.
    mockMvc.perform(post(URL)
            .header("X-User-Id", MEMBER_ID)
            .header("Idempotency-Key", "idem-resub-2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(PRODUCT_ID, METHOD_ID)))
        .andExpect(status().isAccepted());

    assertThat(subscriptionRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("상품 없음: product-service 404 이면 주문이 생성되지 않는다(FeignException 전파)")
  void createSubscription_productNotFound() {
    stubMemberValid(MEMBER_ID);
    stubProductNotFound(PRODUCT_ID);

    // 현재 파사드는 Feign 404 를 도메인 예외로 변환하지 않아 FeignException 이 그대로 전파된다.
    // (전역 핸들러는 AppException 만 처리 → 실서버에선 500. MockMvc 는 예외를 되던진다.)
    // → 별도 이슈: product/member 미존재는 404/400 으로 변환하는 ErrorDecoder 도입 권장.
    assertThatThrownBy(() ->
        mockMvc.perform(post(URL)
            .header("X-User-Id", MEMBER_ID)
            .header("Idempotency-Key", "idem-noproduct-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(PRODUCT_ID, METHOD_ID))))
        .hasRootCauseInstanceOf(FeignException.class);

    // 검증 실패이므로 어떤 도메인 레코드도 남지 않아야 한다
    assertThat(subscriptionRepository.count()).isZero();
    assertThat(subscriptionOrderRepository.count()).isZero();
    assertThat(idempotencyKeyRepository.count()).isZero();
    assertThat(outboxEventRepository.count()).isZero();
  }

  @Test
  @DisplayName("결제 성공 이벤트 소비: 신규 구독이 ACTIVE 로 활성화되고 채팅방 생성 이벤트가 적재된다")
  void consumePaymentSuccess_activatesSubscription() throws Exception {
    stubMemberValid(MEMBER_ID);
    stubProduct(PRODUCT_ID, "아이유 구독권", 9900L, "ACTIVE", PRODUCT_ID);

    MvcResult created = mockMvc.perform(post(URL)
            .header("X-User-Id", MEMBER_ID)
            .header("Idempotency-Key", "idem-activate-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(PRODUCT_ID, METHOD_ID)))
        .andExpect(status().isAccepted())
        .andReturn();
    long orderId = orderId(created);
    long subscriptionId = subscriptionId(created);

    // payment-service 가 발행하는 결제 성공 결과를 모사
    PaymentResultEvent event = new PaymentResultEvent(
        orderId, 999L, BigDecimal.valueOf(9900), "test-payment-key",
        PaymentResultEvent.STATUS_SUCCEEDED, "");
    kafkaTemplate.send(OrderKafkaTopic.PAYMENT_RESULT_SUCCESS, String.valueOf(orderId), event);

    // 비동기 소비 → 구독 ACTIVE
    await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(500)).untilAsserted(() ->
        assertThat(subscriptionRepository.findById(subscriptionId))
            .get()
            .satisfies(s -> assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE)));

    // 주문 COMPLETED
    assertThat(subscriptionOrderRepository.findById(orderId))
        .get()
        .satisfies((SubscriptionOrder o) -> assertThat(o.getStatus()).isEqualTo(OrderStatus.COMPLETED));

    // 채팅방 생성 이벤트(SubscriptionActivated)가 아웃박스에 적재
    assertThat(outboxEventRepository.findTop100ByStatusOrderByIdAsc(OutboxStatus.PENDING))
        .anySatisfy(e -> {
          assertThat(e.getTopic()).isEqualTo(OrderKafkaTopic.SUBSCRIPTION_ACTIVATED);
          assertThat(e.getEventType()).isEqualTo("SubscriptionActivated");
        });
  }

  // ── helpers ──

  private String body(long productId, long methodId) {
    return """
        {"productId":%d,"paymentMethodId":%d}
        """.formatted(productId, methodId);
  }

  private long orderId(MvcResult result) throws Exception {
    return dataNode(result).get("orderId").asLong();
  }

  private long subscriptionId(MvcResult result) throws Exception {
    return dataNode(result).get("subscriptionId").asLong();
  }

  private JsonNode dataNode(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
  }
}
