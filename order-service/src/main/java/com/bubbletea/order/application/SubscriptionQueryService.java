package com.bubbletea.order.application;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.bubbletea.order.presentation.external.dto.SubscriptionResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionQueryService {

  private final BillingScheduleRepository billingScheduleRepository;

  @Transactional(readOnly = true)
  public List<SubscriptionResponseDto> getMemberSubscriptions(Long memberId) {
    return billingScheduleRepository.findAllByMemberId(memberId).stream()
        .map(SubscriptionResponseDto::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public SubscriptionResponseDto getSubscription(Long memberId, Long subscriptionId) {
    return billingScheduleRepository.findBySubscriptionId(subscriptionId)
        // 소유자 검증: 타인의 구독이면 존재를 노출하지 않도록 동일하게 404 처리.
        .filter(schedule -> schedule.getSubscription().getMemberId().equals(memberId))
        .map(SubscriptionResponseDto::from)
        .orElseThrow(() -> new AppException(OrderErrorCode.SUBSCRIPTION_NOT_FOUND));
  }
}
