package com.bubbletea.chat.domain.repository;

import com.bubbletea.chat.domain.entity.ChatPeriod;
import com.bubbletea.chat.domain.enums.ParticipantStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatPeriodRepository extends JpaRepository<ChatPeriod, Long> {

  Optional<ChatPeriod> findTopByRoomIdAndFanIdAndStatusAndStartedAtLessThanEqualOrderByStartedAtDesc(
      Long roomId, Long fanId, ParticipantStatus status, LocalDateTime endedAt);
}
