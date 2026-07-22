package com.bubbletea.order.domain.repository;

import com.bubbletea.order.domain.entity.IdempotencyKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

  Optional<IdempotencyKey> findByMemberIdAndIdempotencyKey(Long memberId, String idempotencyKey);
}
