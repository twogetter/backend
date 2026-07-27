package com.bubbletea.order.domain.repository;

import com.bubbletea.order.domain.entity.Subscription;
import com.bubbletea.order.domain.enums.SubscriptionStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

  /**
   * 같은 회원이 같은 상품을 이미 점유(구독 또는 처리 중)하고 있는지 확인한다.
   *
   * <p>엔티티에 {@code @SQLRestriction("deleted_at IS NULL")} 이 걸려 있어 취소(소프트 삭제)된 구독은
   * 자동으로 제외되므로, 해지 후 재구독은 정상 허용된다.
   */
  boolean existsByMemberIdAndProductIdAndStatusIn(
      Long memberId, Long productId, Collection<SubscriptionStatus> statuses);
}
