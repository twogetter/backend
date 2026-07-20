package com.bubbletea.order.application;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import com.bubbletea.order.domain.event.SubscriptionRenewalEvent;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.bubbletea.order.infrastructure.kafka.OrderKafkaTopic;
import com.bubbletea.order.infrastructure.outbox.OutboxRecorder;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RenewalReminderService {

  private static final String AGGREGATE_TYPE = "Subscription";
  private static final String EVENT_TYPE = "RenewalReminder";

  @Value("${order.renewal.remind-days-before:3}")
  private int remindDaysBefore;

  private final BillingScheduleRepository billingScheduleRepository;
  private final OutboxRecorder outboxRecorder;

  @Transactional
  public void sendImminentRenewalReminders() {
    // 기준일은 한 번만 캡처해 조회(오늘+N)와 daysLeft가 자정 경계에서 어긋나지 않게 한다.
    LocalDate today = LocalDate.now();
    LocalDate targetDate = today.plusDays(remindDaysBefore);

    List<BillingSchedule> schedules =
        billingScheduleRepository.findByStatusAndNextBillingDate(SubscriptionStatus.ACTIVE, targetDate);
    if (schedules.isEmpty()) {
      return;
    }
    log.info("[RenewalReminder] 임박 알림 대상 {}건 (결제예정일={})", schedules.size(), targetDate);

    for (BillingSchedule schedule : schedules) {
      Subscription subscription = schedule.getSubscription();
      SubscriptionRenewalEvent event = SubscriptionRenewalEvent.of(
          subscription.getMemberId(), subscription.getProductName(), schedule.getAmount(),
          remindDaysBefore, schedule.getNextBillingDate());
      outboxRecorder.record(AGGREGATE_TYPE, subscription.getId(), EVENT_TYPE,
          OrderKafkaTopic.RENEWAL_SUBSCRIPTION, String.valueOf(subscription.getMemberId()), event);
    }
    log.info("[RenewalReminder] 임박 알림 아웃박스 기록 완료. {}건", schedules.size());
  }
}
