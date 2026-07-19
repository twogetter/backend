package com.bubbletea.order.infrastructure.scheduler;

import com.bubbletea.order.application.RecurringBillingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기결제 배치 트리거. 결제 예정일이 도래한 스케줄을 조회해 스케줄별로 결제요청을 발행한다.
 * 스케줄별 트랜잭션 분리를 위해 프록시 경유로 {@link RecurringBillingService#processDueSchedule(Long)}를 호출한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringBillingScheduler {

  private final RecurringBillingService recurringBillingService;

  @Scheduled(cron = "${order.billing.batch-cron:0 0 3 * * *}")
  public void runDueBillings() {
    List<Long> dueIds = recurringBillingService.findDueScheduleIds();
    if (dueIds.isEmpty()) {
      return;
    }
    log.info("[Billing] 정기결제 배치 시작. 대상 {}건", dueIds.size());

    int success = 0;
    for (Long scheduleId : dueIds) {
      try {
        recurringBillingService.processDueSchedule(scheduleId);
        success++;
      } catch (Exception e) {
        // 한 건 실패가 배치 전체를 막지 않도록 격리(다음 배치 주기에 재시도).
        log.error("[Billing] 스케줄 처리 실패. scheduleId={}, error={}", scheduleId, e.getMessage(), e);
      }
    }
    log.info("[Billing] 정기결제 배치 종료. 성공 {}/{}", success, dueIds.size());
  }
}
