package com.bubbletea.order.domain.repository;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingScheduleRepository extends JpaRepository<BillingSchedule, Long> {

  /** 결제 예정일이 도래한(오늘 이하) 활성 스케줄 = 정기결제 배치 대상. */
  List<BillingSchedule> findByStatusAndNextBillingDateLessThanEqual(
      SubscriptionStatus status, LocalDate date);

  /** 결제 예정일이 특정일(오늘+N)인 활성 스케줄 = 정기결제 임박 알림 대상(정확히 하루만 매칭 → 중복 알림 방지). */
  List<BillingSchedule> findByStatusAndNextBillingDate(SubscriptionStatus status, LocalDate date);
}
