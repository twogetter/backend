package com.bubbletea.order.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.bubbletea.commontest.container.KafkaTestContainer;
import com.bubbletea.commontest.container.PostgresTestContainer;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * order-service 통합 테스트 베이스.
 *
 * <p>외부 동기 의존은 두 Feign 클라이언트뿐이다(user-service, product-service).
 * 두 클라이언트는 {@code @FeignClient(name=...)} 로 서비스 디스커버리(Eureka/LoadBalancer)를 통해
 * 해석되므로, payment-service 처럼 {@code base-url} 프로퍼티를 WireMock 으로 돌릴 수 없다.
 * 대신 <b>SimpleDiscoveryClient</b> 의 정적 인스턴스를 단일 WireMock 서버로 지정해
 * 프로덕션 코드 수정 없이 Feign 호출을 스텁으로 유도한다.
 *
 * <p>두 서비스가 서로 다른 경로를 쓰므로({@code /internal/members/**} vs
 * {@code /api/v1/products/**}) 하나의 WireMock 서버가 양쪽을 모두 처리한다.
 *
 * <p>인프라: Postgres·Kafka 는 공용 Testcontainers 로 기동한다.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class OrderIntegrationTestSupport
    implements PostgresTestContainer, KafkaTestContainer {

  protected static final WireMockServer WIREMOCK = new WireMockServer(
      WireMockConfiguration.wireMockConfig().dynamicPort());

  static {
    WIREMOCK.start();
  }

  @DynamicPropertySource
  static void discoveryProperties(DynamicPropertyRegistry registry) {
    // user-service·product-service 를 동일 WireMock 인스턴스로 해석시킨다.
    registry.add(
        "spring.cloud.discovery.client.simple.instances.user-service[0].uri",
        WIREMOCK::baseUrl);
    registry.add(
        "spring.cloud.discovery.client.simple.instances.product-service[0].uri",
        WIREMOCK::baseUrl);
  }

  /** 스키마 이름은 {@code order-service/src/main/resources/db/subscription-unique-index.sql} 와 일치시킨다. */
  private static final String SUBSCRIPTION_UNIQUE_INDEX = """
      CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_member_product_active
        ON subscriptions (member_id, product_id) WHERE deleted_at IS NULL
      """;

  private static final String TRUNCATE_ALL = """
      TRUNCATE TABLE payment_attempts, subscription_order, billing_schedules,
                     subscriptions, idempotency_keys, outbox_events
      RESTART IDENTITY CASCADE
      """;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetWireMock() {
    WIREMOCK.resetAll();
  }

  /**
   * 컨텍스트(=Postgres 컨테이너)를 테스트끼리 공유하므로 매 테스트마다 도메인 테이블을 비운다.
   * 비우지 않으면 이전 테스트가 남긴 구독이 중복 구독 검증에 걸려 뒤 테스트가 409 로 실패한다.
   *
   * <p>부분 유니크 인덱스는 JPA(ddl-auto)로 생성되지 않으므로 여기서 함께 만들어,
   * 운영 스키마와 동일한 제약 위에서 테스트가 돌게 한다.
   */
  @BeforeEach
  void resetDatabase() {
    jdbcTemplate.execute(TRUNCATE_ALL);
    jdbcTemplate.execute(SUBSCRIPTION_UNIQUE_INDEX);
  }

  // ── 스텁 헬퍼 ─────────────────────────────────────────────

  /** 회원 검증 성공(존재). MemberClient.validateMember 는 200 이면 통과. */
  protected void stubMemberValid(long memberId) {
    WIREMOCK.stubFor(get(urlEqualTo("/internal/members/" + memberId))
        .willReturn(aResponse().withStatus(200)));
  }

  /** 회원 없음(404) → FeignException. */
  protected void stubMemberNotFound(long memberId) {
    WIREMOCK.stubFor(get(urlEqualTo("/internal/members/" + memberId))
        .willReturn(aResponse().withStatus(404)));
  }

  /** 상품 검증 응답. JSON 필드는 ProductInfoResponseDto(pid,name,price,productStatus,artistId) 와 일치. */
  protected void stubProduct(long productId, String name, long price, String status, long artistId) {
    String body = """
        {"pid":%d,"name":"%s","price":%d,"productStatus":"%s","artistId":%d}
        """.formatted(productId, name, price, status, artistId);
    WIREMOCK.stubFor(get(urlEqualTo("/api/v1/products/" + productId + "/validation"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(body)));
  }

  /** 상품 없음(404) → FeignException. */
  protected void stubProductNotFound(long productId) {
    WIREMOCK.stubFor(get(urlEqualTo("/api/v1/products/" + productId + "/validation"))
        .willReturn(aResponse().withStatus(404)));
  }
}
