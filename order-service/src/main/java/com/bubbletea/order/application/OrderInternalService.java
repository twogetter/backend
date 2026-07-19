package com.bubbletea.order.application;

import com.bubbletea.common.exception.AppException;
import com.bubbletea.order.domain.entity.SubscriptionOrder;
import com.bubbletea.order.domain.exception.OrderErrorCode;
import com.bubbletea.order.domain.repository.SubscriptionOrderRepository;
import com.bubbletea.order.presentation.internal.dto.OrderAmountResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderInternalService {

  private final SubscriptionOrderRepository subscriptionOrderRepository;

  @Transactional(readOnly = true)
  public OrderAmountResponseDto getOrderAmount(Long orderId) {
    SubscriptionOrder order = subscriptionOrderRepository.findById(orderId)
        .orElseThrow(() -> new AppException(OrderErrorCode.ORDER_NOT_FOUND));
    Long memberId = order.getBillingSchedule().getSubscription().getMemberId();
    return OrderAmountResponseDto.of(memberId, orderId, order.getAmount());
  }
}
