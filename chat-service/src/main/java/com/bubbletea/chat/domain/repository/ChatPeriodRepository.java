package com.bubbletea.chat.domain.repository;

import com.bubbletea.chat.domain.entity.ChatPeriod;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatPeriodRepository extends JpaRepository<ChatPeriod, Long> {

  Optional<ChatPeriod> findTopByRoomIdAndFanIdOrderByStartedAtDesc(Long roomId, Long fanId);
}
