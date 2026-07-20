package com.bubbletea.order.application;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import com.bubbletea.order.domain.event.SubscriptionRenewalEvent;
import com.bubbletea.order.domain.repository.BillingScheduleRepository;
import com.bubbletea.order.infrastructure.kafka.producer.OrderEventPublisher;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정기결제 임박 알림 발행.
 * 결제 예정일이 정확히 (오늘 + N일)인 활성 스케줄을 찾아 회원별로 {@link SubscriptionRenewalEvent}를 발행한다.
 * 정확히 하루만 매칭하므로 배치가 하루 1회 실행되면 스케줄당 알림은 1회만 발송된다(중복 방지).
 * 상태 변경이 없는 순수 read → publish라 아웃박스(dual-write) 대상이 아니며 직접 발행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RenewalReminderService {

  @Value("${order.renewal.remind-days-before:3}")
  private int remindDaysBefore;

  private final BillingScheduleRepository billingScheduleRepository;
  private final OrderEventPublisher orderEventPublisher;

  @Transactional(readOnly = true)
  public void sendImminentRenewalReminders() {
    LocalDate targetDate = LocalDate.now().plusDays(remindDaysBefore);
    List<BillingSchedule> schedules =
        billingScheduleRepository.findByStatusAndNextBillingDate(SubscriptionStatus.ACTIVE, targetDate);
    if (schedules.isEmpty()) {
      return;
    }
    log.info("[RenewalReminder] 임박 알림 대상 {}건 (결제예정일={})", schedules.size(), targetDate);

    int sent = 0;
    for (BillingSchedule schedule : schedules) {
      try {
        Subscription subscription = schedule.getSubscription();
        int daysLeft = (int) ChronoUnit.DAYS.between(LocalDate.now(), schedule.getNextBillingDate());
        orderEventPublisher.sendSubscriptionRenewalEvent(SubscriptionRenewalEvent.of(
            subscription.getMemberId(), subscription.getProductName(), schedule.getAmount(),
            daysLeft, schedule.getNextBillingDate()));
        sent++;
      } catch (Exception e) {
        // 한 건 실패가 배치 전체를 막지 않도록 격리(알림은 순수 발행이라 다음 실행 재시도는 하지 않음).
        log.error("[RenewalReminder] 알림 발행 실패. scheduleId={}, error={}", schedule.getId(), e.getMessage(), e);
      }
    }
    log.info("[RenewalReminder] 임박 알림 발행 완료. 성공 {}/{}", sent, schedules.size());
  }
}
