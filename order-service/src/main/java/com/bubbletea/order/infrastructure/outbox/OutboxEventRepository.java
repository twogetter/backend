package com.bubbletea.order.infrastructure.outbox;

import com.bubbletea.order.domain.entity.OutboxEvent;
import com.bubbletea.order.domain.enums.OutboxStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

  List<OutboxEvent> findTop100ByStatusOrderByIdAsc(OutboxStatus status);
}
