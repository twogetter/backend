package com.bubbletea.order.domain.repository;

import com.bubbletea.order.domain.entity.BillingSchedule;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingScheduleRepository extends JpaRepository<BillingSchedule, Long> {

  /** 결제 예정일이 도래한(오늘 이하) 활성 스케줄 = 정기결제 배치 대상. */
  List<BillingSchedule> findByStatusAndNextBillingDateLessThanEqual(
      SubscriptionStatus status, LocalDate date);

  /** 결제 예정일이 특정일(오늘+N)인 활성 스케줄 = 정기결제 임박 알림 대상(정확히 하루만 매칭 → 중복 알림 방지). */
  List<BillingSchedule> findByStatusAndNextBillingDate(SubscriptionStatus status, LocalDate date);

  /** 회원의 구독 목록(스케줄+구독 fetch join). 소프트 삭제(취소)는 @SQLRestriction으로 자동 제외. */
  @Query("select bs from BillingSchedule bs join fetch bs.subscription s "
      + "where s.memberId = :memberId order by s.startedAt desc")
  List<BillingSchedule> findAllByMemberId(@Param("memberId") Long memberId);

  /** 단건 구독(스케줄+구독 fetch join). */
  @Query("select bs from BillingSchedule bs join fetch bs.subscription s where s.id = :subscriptionId")
  Optional<BillingSchedule> findBySubscriptionId(@Param("subscriptionId") Long subscriptionId);
}
