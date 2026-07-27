package com.bubbletea.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.order.domain.dto.CreatedOrderContextDto;
import com.bubbletea.order.domain.entity.IdempotencyKey;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import com.bubbletea.order.domain.repository.IdempotencyKeyRepository;
import com.bubbletea.order.domain.repository.SubscriptionRepository;
import com.bubbletea.order.infrastructure.client.MemberClient;
import com.bubbletea.order.infrastructure.client.ProductClient;
import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import com.bubbletea.order.presentation.external.dto.OrderResponseDto;
import com.bubbletea.order.presentation.external.dto.SubscriptionCreateRequestDto;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionOrderFacade — 구독 생성 오케스트레이션")
class SubscriptionOrderFacadeTest {

  private static final Long MEMBER_ID = 1L;
  private static final String IDEMPOTENCY_KEY = "idem-key-1";
  private static final Long PRODUCT_ID = 10L;
  private static final Long METHOD_ID = 5L;

  @Mock
  private MemberClient memberClient;
  @Mock
  private ProductClient productClient;
  @Mock
  private OrderCreationService orderCreationService;
  @Mock
  private IdempotencyKeyRepository idempotencyKeyRepository;
  @Mock
  private SubscriptionRepository subscriptionRepository;

  @InjectMocks
  private SubscriptionOrderFacade facade;

  /** 중복 구독 선검사를 "점유 없음"으로 통과시킨다. */
  private void stubNotOccupied() {
    when(subscriptionRepository
        .existsByMemberIdAndProductIdAndStatusIn(eq(MEMBER_ID), eq(PRODUCT_ID), any()))
        .thenReturn(false);
  }

  private SubscriptionCreateRequestDto request() {
    return new SubscriptionCreateRequestDto(PRODUCT_ID, METHOD_ID);
  }

  private ProductInfoResponseDto product() {
    return new ProductInfoResponseDto(PRODUCT_ID, "구독권", 9900L, "ACTIVE", PRODUCT_ID);
  }

  private CreatedOrderContextDto context() {
    return new CreatedOrderContextDto(200L, 300L, 100L, MEMBER_ID, PRODUCT_ID,
        BigDecimal.valueOf(9900));
  }

  @Test
  @DisplayName("동일 멱등키 재요청이면 검증·생성 없이 기존 주문을 반환한다")
  void idempotentFastPath() {
    IdempotencyKey existing = new IdempotencyKey(MEMBER_ID, IDEMPOTENCY_KEY, 100L, 200L);
    when(idempotencyKeyRepository.findByMemberIdAndIdempotencyKey(MEMBER_ID, IDEMPOTENCY_KEY))
        .thenReturn(Optional.of(existing));

    OrderResponseDto response = facade.createSubscription(MEMBER_ID, IDEMPOTENCY_KEY, request());

    assertThat(response.orderId()).isEqualTo(100L);
    assertThat(response.subscriptionId()).isEqualTo(200L);
    assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
    verifyNoInteractions(memberClient, productClient, orderCreationService);
  }

  @Test
  @DisplayName("신규 요청이면 회원 검증·상품 조회 후 주문을 생성하고 PENDING 응답을 반환한다")
  void createsNewSubscription() {
    when(idempotencyKeyRepository.findByMemberIdAndIdempotencyKey(MEMBER_ID, IDEMPOTENCY_KEY))
        .thenReturn(Optional.empty());
    stubNotOccupied();
    ProductInfoResponseDto product = product();
    when(productClient.getProductInfo(PRODUCT_ID)).thenReturn(product);
    when(orderCreationService.createPendingOrder(MEMBER_ID, product, METHOD_ID, IDEMPOTENCY_KEY))
        .thenReturn(context());

    OrderResponseDto response = facade.createSubscription(MEMBER_ID, IDEMPOTENCY_KEY, request());

    assertThat(response.orderId()).isEqualTo(100L);
    assertThat(response.subscriptionId()).isEqualTo(200L);
    assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
    verify(memberClient).validateMember(MEMBER_ID);
    verify(productClient).getProductInfo(PRODUCT_ID);
    verify(orderCreationService).createPendingOrder(MEMBER_ID, product, METHOD_ID, IDEMPOTENCY_KEY);
  }

  @Test
  @DisplayName("이미 점유 중인 상품이면 외부 호출 없이 409(DUPLICATE_SUBSCRIPTION)로 거절한다")
  void rejectsDuplicateSubscription() {
    when(idempotencyKeyRepository.findByMemberIdAndIdempotencyKey(MEMBER_ID, IDEMPOTENCY_KEY))
        .thenReturn(Optional.empty());
    when(subscriptionRepository
        .existsByMemberIdAndProductIdAndStatusIn(eq(MEMBER_ID), eq(PRODUCT_ID), any()))
        .thenReturn(true);

    assertThatThrownBy(() -> facade.createSubscription(MEMBER_ID, IDEMPOTENCY_KEY, request()))
        .isInstanceOf(AppException.class)
        .satisfies(e -> assertThat(((AppException) e).getErrorCode())
            .isEqualTo(OrderErrorCode.DUPLICATE_SUBSCRIPTION));

    // 선검사가 외부 호출보다 앞에 있어야 불필요한 Feign 왕복이 없다
    verifyNoInteractions(memberClient, productClient, orderCreationService);
  }

  @Test
  @DisplayName("동시 중복 요청으로 유니크 제약 위반 시 기존 주문을 반환한다")
  void returnsExistingOnDataIntegrityViolation() {
    IdempotencyKey existing = new IdempotencyKey(MEMBER_ID, IDEMPOTENCY_KEY, 100L, 200L);
    when(idempotencyKeyRepository.findByMemberIdAndIdempotencyKey(MEMBER_ID, IDEMPOTENCY_KEY))
        .thenReturn(Optional.empty())      // fast-path 통과
        .thenReturn(Optional.of(existing)); // 커밋 경쟁 후 재조회
    stubNotOccupied();
    when(productClient.getProductInfo(PRODUCT_ID)).thenReturn(product());
    when(orderCreationService.createPendingOrder(any(), any(), any(), any()))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    OrderResponseDto response = facade.createSubscription(MEMBER_ID, IDEMPOTENCY_KEY, request());

    assertThat(response.orderId()).isEqualTo(100L);
    assertThat(response.subscriptionId()).isEqualTo(200L);
    assertThat(response.status()).isEqualTo(OrderStatus.PENDING.name());
  }
}
