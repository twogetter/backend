package com.bubbletea.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.bubbletea.order.presentation.external.dto.SubscriptionResponseDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionQueryService — 구독 조회")
class SubscriptionQueryServiceTest {

  private static final Long MEMBER_ID = 1L;
  private static final Long SUBSCRIPTION_ID = 200L;

  @Mock
  private BillingScheduleRepository billingScheduleRepository;

  @InjectMocks
  private SubscriptionQueryService subscriptionQueryService;

  private BillingSchedule scheduleOwnedBy(Long memberId) {
    Subscription subscription = new Subscription(memberId, 10L, "구독권");
    return new BillingSchedule(subscription, 5L, BigDecimal.valueOf(9900));
  }

  @Test
  @DisplayName("회원의 구독 목록을 DTO 로 매핑해 반환한다")
  void getMemberSubscriptions() {
    when(billingScheduleRepository.findAllByMemberId(MEMBER_ID))
        .thenReturn(List.of(scheduleOwnedBy(MEMBER_ID)));

    List<SubscriptionResponseDto> result =
        subscriptionQueryService.getMemberSubscriptions(MEMBER_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).productName()).isEqualTo("구독권");
    assertThat(result.get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(9900));
  }

  @Test
  @DisplayName("본인 구독이면 단건 조회에 성공한다")
  void getSubscriptionByOwner() {
    when(billingScheduleRepository.findBySubscriptionId(SUBSCRIPTION_ID))
        .thenReturn(Optional.of(scheduleOwnedBy(MEMBER_ID)));

    SubscriptionResponseDto result =
        subscriptionQueryService.getSubscription(MEMBER_ID, SUBSCRIPTION_ID);

    assertThat(result.productName()).isEqualTo("구독권");
  }

  @Test
  @DisplayName("타인의 구독은 존재를 노출하지 않고 SUBSCRIPTION_NOT_FOUND 로 처리한다")
  void getSubscriptionByNonOwnerThrowsNotFound() {
    when(billingScheduleRepository.findBySubscriptionId(SUBSCRIPTION_ID))
        .thenReturn(Optional.of(scheduleOwnedBy(999L)));

    assertThatThrownBy(() -> subscriptionQueryService.getSubscription(MEMBER_ID, SUBSCRIPTION_ID))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(OrderErrorCode.SUBSCRIPTION_NOT_FOUND);
  }

  @Test
  @DisplayName("존재하지 않는 구독은 SUBSCRIPTION_NOT_FOUND 를 던진다")
  void getSubscriptionMissingThrowsNotFound() {
    when(billingScheduleRepository.findBySubscriptionId(SUBSCRIPTION_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> subscriptionQueryService.getSubscription(MEMBER_ID, SUBSCRIPTION_ID))
        .isInstanceOf(AppException.class)
        .extracting(e -> ((AppException) e).getErrorCode())
        .isEqualTo(OrderErrorCode.SUBSCRIPTION_NOT_FOUND);
  }
}
