package com.bubbletea.order.infrastructure.scheduler;

import com.bubbletea.order.application.RenewalReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기결제 임박 알림 배치 트리거. 하루 1회 실행해 결제 예정일이 (오늘 + N일)인 구독에 알림을 발행한다.
 */
@Component
@RequiredArgsConstructor
public class RenewalReminderScheduler {

  private final RenewalReminderService renewalReminderService;

  @Scheduled(cron = "${order.renewal.remind-cron:0 0 9 * * *}")
  public void runRenewalReminders() {
    renewalReminderService.sendImminentRenewalReminders();
  }
}
