package com.bubbletea.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bubbletea.order.domain.dto.CreatedOrderContextDto;
import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.enums.OrderStatus;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.bubbletea.order.domain.repository.IdempotencyKeyRepository;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import com.bubbletea.order.domain.repository.SubscriptionRepository;
import com.bubbletea.order.infrastructure.client.dto.ProductInfoResponseDto;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCreationService — PENDING 주문 생성(TX1)")
class OrderCreationServiceTest {

  private static final Long MEMBER_ID = 1L;
  private static final Long PRODUCT_ID = 10L;
  private static final Long METHOD_ID = 5L;
  private static final String IDEMPOTENCY_KEY = "idem-key-1";

  @Mock
  private SubscriptionRepository subscriptionRepository;
  @Mock
  private BillingScheduleRepository billingScheduleRepository;
  @Mock
  private SubscriptionOrderRepository subscriptionOrderRepository;
  @Mock
  private IdempotencyKeyRepository idempotencyKeyRepository;
  @Mock
  private BillingRequestPublisher billingRequestPublisher;

  @InjectMocks
  private OrderCreationService orderCreationService;

  private ProductInfoResponseDto product() {
    return new ProductInfoResponseDto(PRODUCT_ID, "구독권", 9900L, "ACTIVE", PRODUCT_ID);
  }

  @Test
  @DisplayName("구독·스케줄·주문·멱등키를 저장하고 결제요청을 발행하며 컨텍스트를 반환한다")
  void createPendingOrder() {
    CreatedOrderContextDto context = orderCreationService.createPendingOrder(
        MEMBER_ID, product(), METHOD_ID, IDEMPOTENCY_KEY);

    // 저장 검증 + 저장된 엔티티 값 확인
    ArgumentCaptor<Subscription> subscription = ArgumentCaptor.forClass(Subscription.class);
    verify(subscriptionRepository).save(subscription.capture());
    assertThat(subscription.getValue().getMemberId()).isEqualTo(MEMBER_ID);
    assertThat(subscription.getValue().getProductId()).isEqualTo(PRODUCT_ID);
    assertThat(subscription.getValue().getProductName()).isEqualTo("구독권");

    ArgumentCaptor<BillingSchedule> schedule = ArgumentCaptor.forClass(BillingSchedule.class);
    verify(billingScheduleRepository).save(schedule.capture());
    assertThat(schedule.getValue().getPaymentMethodId()).isEqualTo(METHOD_ID);
    assertThat(schedule.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(9900));

    ArgumentCaptor<SubscriptionOrder> order = ArgumentCaptor.forClass(SubscriptionOrder.class);
    verify(subscriptionOrderRepository).save(order.capture());
    assertThat(order.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);

    verify(idempotencyKeyRepository).save(org.mockito.ArgumentMatchers.any());
    // 결제요청 아웃박스 발행(주문 커밋과 동일 트랜잭션)
    verify(billingRequestPublisher).publish(order.getValue());

    // 반환 컨텍스트
    assertThat(context.memberId()).isEqualTo(MEMBER_ID);
    assertThat(context.productId()).isEqualTo(PRODUCT_ID);
    assertThat(context.amount()).isEqualByComparingTo(BigDecimal.valueOf(9900));
  }
}
